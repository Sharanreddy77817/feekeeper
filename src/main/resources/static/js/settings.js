document.addEventListener('DOMContentLoaded', function () {
    'use strict';
    if (!FeeKeeperApi.requireSession()) return;
    const form = document.getElementById('settings-form');
    const phonePattern = /^(?:\+91)?[6-9]\d{9}$/;
    const upiPattern = /^[A-Za-z0-9._-]+@[A-Za-z][A-Za-z0-9.-]+$/;

    const themeCards = document.querySelectorAll('.theme-card');
    themeCards.forEach(function (card) {
        card.addEventListener('click', function () {
            const radio = card.querySelector('input[type="radio"]');
            if (radio) {
                radio.checked = true;
                updateThemeCards(radio.value);
            }
        });
    });

    function updateThemeCards(selectedValue) {
        themeCards.forEach(function (c) {
            const isMatch = c.getAttribute('data-theme') === selectedValue;
            const status = c.querySelector('.theme-status');
            const radio = c.querySelector('input[type="radio"]');
            if (radio) radio.checked = isMatch;
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
                    status.textContent = 'Select';
                    status.classList.remove('text-indigo-600');
                    status.classList.add('text-slate-400');
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
            const theme = settings.receiptTheme || 'ANDROMEDA';
            updateThemeCards(theme);
            setVisible('settings-form');
        } catch (error) { if (error.status !== 401) setVisible('settings-error'); }
    }

    async function save(event) {
        event.preventDefault(); FeeKeeperApi.clearFieldErrors(form); hide('settings-form-error');
        const checkedTheme = form.querySelector('input[name="receiptTheme"]:checked');
        const value = {
            businessName: form.businessName.value.trim(),
            ownerName: form.ownerName.value.trim(),
            phone: form.phone.value.trim(),
            upiId: form.upiId.value.trim() || null,
            currencyCode: form.currencyCode.value,
            timeZone: form.timeZone.value,
            receiptTheme: checkedTheme ? checkedTheme.value : 'ANDROMEDA'
        };
        const invalid = validate(value); if (invalid) { fieldError(invalid[0], invalid[1]); return; }
        const button = document.getElementById('save-settings'); button.disabled = true; button.textContent = 'Saving…';
        try {
            const saved = await FeeKeeperApi.request('/api/tenant/settings', {method: 'PUT', body: value});
            form.businessName.value = saved.businessName; form.ownerName.value = saved.ownerName;
            form.phone.value = saved.phone; form.upiId.value = saved.upiId || '';
            form.currencyCode.value = saved.currencyCode; form.timeZone.value = saved.timeZone;
            updateThemeCards(saved.receiptTheme || 'ANDROMEDA');
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
