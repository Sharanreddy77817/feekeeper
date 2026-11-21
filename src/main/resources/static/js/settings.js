document.addEventListener('DOMContentLoaded', function () {
    'use strict';
    if (!FeeKeeperApi.requireSession()) return;
    const form = document.getElementById('settings-form');
    const phonePattern = /^(?:\+91)?[6-9]\d{9}$/;
    const upiPattern = /^[A-Za-z0-9._-]+@[A-Za-z][A-Za-z0-9.-]+$/;

    const state = {
        unlockedThemes: 'ANDROMEDA',
        pendingUnlockTheme: null
    };

    const unlockBanner = document.getElementById('unlock-theme-banner');
    const unlockName = document.getElementById('unlock-theme-name');
    const btnUnlock = document.getElementById('btn-unlock-theme');

    const themeCards = document.querySelectorAll('.theme-card');
    themeCards.forEach(function (card) {
        card.addEventListener('click', function () {
            const targetTheme = card.getAttribute('data-theme');
            const hasAccess = isThemeUnlocked(targetTheme);

            if (!hasAccess) {
                state.pendingUnlockTheme = targetTheme;
                if (unlockName) unlockName.textContent = targetTheme === 'NEBULA' ? 'Cosmic Nebula Pass' : 'Milky Way Starlight';
                if (unlockBanner) unlockBanner.classList.remove('hidden');
                return;
            }

            if (unlockBanner) unlockBanner.classList.add('hidden');
            state.pendingUnlockTheme = null;
            const radio = card.querySelector('input[type="radio"]');
            if (radio) {
                radio.checked = true;
                updateThemeCards(radio.value, state.unlockedThemes);
            }
        });
    });

    if (btnUnlock) {
        btnUnlock.addEventListener('click', async function () {
            if (!state.pendingUnlockTheme) return;
            btnUnlock.disabled = true;
            btnUnlock.textContent = 'Unlocking…';
            try {
                const updated = await FeeKeeperApi.request('/api/tenant/settings/unlock-theme?theme=' + state.pendingUnlockTheme, {method: 'POST'});
                state.unlockedThemes = updated.unlockedThemes || (state.unlockedThemes + ',' + state.pendingUnlockTheme);
                updateThemeCards(updated.receiptTheme || state.pendingUnlockTheme, state.unlockedThemes);
                if (unlockBanner) unlockBanner.classList.add('hidden');
                state.pendingUnlockTheme = null;
                FeeKeeperApi.showToast('Premium receipt design unlocked (+₹30)!');
            } catch (err) {
                FeeKeeperApi.showToast(err.message, 'error');
            } finally {
                btnUnlock.disabled = false;
                btnUnlock.textContent = 'Unlock for ₹30';
            }
        });
    }

    function isThemeUnlocked(theme) {
        if (!theme || theme === 'ANDROMEDA') return true;
        const list = (state.unlockedThemes || 'ANDROMEDA').split(',').map(s => s.trim().toUpperCase());
        return list.includes(theme.toUpperCase()) || list.includes('ALL');
    }

    function updateThemeCards(selectedValue, unlockedList) {
        themeCards.forEach(function (c) {
            const theme = c.getAttribute('data-theme');
            const isMatch = theme === selectedValue;
            const status = c.querySelector('.theme-status');
            const radio = c.querySelector('input[type="radio"]');
            if (radio) radio.checked = isMatch;
            const unlocked = isThemeUnlocked(theme);

            if (isMatch) {
                c.classList.remove('border-slate-200', 'bg-white');
                c.classList.add('border-indigo-600', 'bg-indigo-50/40', 'shadow-xs');
                if (status) {
                    status.textContent = 'Selected ✓';
                    status.classList.remove('text-slate-400');
                    status.classList.add('text-indigo-600');
                }
            } else {
                c.classList.remove('border-indigo-600', 'bg-indigo-50/40', 'shadow-xs');
                c.classList.add('border-slate-200', 'bg-white');
                if (status) {
                    if (unlocked) {
                        status.textContent = 'Select';
                        status.classList.remove('text-indigo-600', 'text-amber-600');
                        status.classList.add('text-slate-400');
                    } else {
                        status.textContent = '🔒 +₹30';
                        status.classList.remove('text-slate-400', 'text-indigo-600');
                        status.classList.add('text-amber-600');
                    }
                }
            }
        });
    }

    document.getElementById('retry-settings').addEventListener('click', load);
    document.getElementById('logout').addEventListener('click', FeeKeeperApi.logout);
    form.addEventListener('submit', save);
    load();

    async function load() {
        setVisible('settings-loading');
        try {
            const settings = await FeeKeeperApi.request('/api/tenant/settings');
            form.businessName.value = settings.businessName || '';
            form.ownerName.value = settings.ownerName || '';
            form.phone.value = settings.phone || '';
            form.upiId.value = settings.upiId || '';
            form.currencyCode.value = settings.currencyCode || 'INR';
            form.timeZone.value = settings.timeZone || 'Asia/Kolkata';
            state.unlockedThemes = settings.unlockedThemes || 'ANDROMEDA';
            const theme = settings.receiptTheme || 'ANDROMEDA';
            updateThemeCards(theme, state.unlockedThemes);
            setVisible('settings-form');
        } catch (error) { if (error.status !== 401) setVisible('settings-error'); }
    }

    async function save(event) {
        event.preventDefault(); FeeKeeperApi.clearFieldErrors(form); hide('settings-form-error');
        const checkedTheme = form.querySelector('input[name="receiptTheme"]:checked');
        const desiredTheme = checkedTheme ? checkedTheme.value : 'ANDROMEDA';

        if (!isThemeUnlocked(desiredTheme)) {
            state.pendingUnlockTheme = desiredTheme;
            if (unlockName) unlockName.textContent = desiredTheme === 'NEBULA' ? 'Cosmic Nebula Pass' : 'Milky Way Starlight';
            if (unlockBanner) unlockBanner.classList.remove('hidden');
            FeeKeeperApi.showToast('Please unlock this premium receipt design (+₹30) first.', 'error');
            return;
        }

        const value = {
            businessName: form.businessName.value.trim(),
            ownerName: form.ownerName.value.trim(),
            phone: form.phone.value.trim(),
            upiId: form.upiId.value.trim() || null,
            currencyCode: form.currencyCode.value,
            timeZone: form.timeZone.value,
            receiptTheme: desiredTheme
        };
        const invalid = validate(value); if (invalid) { fieldError(invalid[0], invalid[1]); return; }
        const button = document.getElementById('save-settings'); button.disabled = true; button.textContent = 'Saving…';
        try {
            const saved = await FeeKeeperApi.request('/api/tenant/settings', {method: 'PUT', body: value});
            form.businessName.value = saved.businessName; form.ownerName.value = saved.ownerName;
            form.phone.value = saved.phone; form.upiId.value = saved.upiId || '';
            form.currencyCode.value = saved.currencyCode; form.timeZone.value = saved.timeZone;
            state.unlockedThemes = saved.unlockedThemes || state.unlockedThemes;
            updateThemeCards(saved.receiptTheme || 'ANDROMEDA', state.unlockedThemes);
            FeeKeeperApi.showToast('Settings updated.');
        } catch (error) {
            FeeKeeperApi.showFieldErrors(form, error.problem && error.problem.fieldErrors);
            const box = document.getElementById('settings-form-error'); box.textContent = error.message; box.hidden = false; box.classList.remove('hidden');
        } finally { button.disabled = false; button.textContent = 'Save changes'; }
    }

    function validate(value) {
        if (!value.businessName) return ['businessName', 'Business name is required.'];
        if (!value.ownerName) return ['ownerName', 'Owner name is required.'];
        if (!phonePattern.test(value.phone)) return ['phone', 'Enter a valid Indian mobile number.'];
        if (value.upiId && !upiPattern.test(value.upiId)) return ['upiId', 'Enter a valid UPI ID such as name@bank.'];
        return null;
    }
    function fieldError(name, text) { const message = form.querySelector('[data-error-for="' + name + '"]'); message.textContent = text; message.hidden = false; message.classList.remove('hidden'); form.elements.namedItem(name).focus(); }
    function setVisible(id) { ['settings-loading', 'settings-error', 'settings-form'].forEach(function (candidate) { const element = document.getElementById(candidate); element.hidden = candidate !== id; if (candidate === id) element.classList.remove('hidden'); }); }
    function hide(id) { document.getElementById(id).hidden = true; }
});
