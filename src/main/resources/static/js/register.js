document.addEventListener('DOMContentLoaded', function () {
    'use strict';
    if (FeeKeeperApi.token()) { window.location.replace('/dashboard'); return; }
    const form = document.getElementById('register-form');
    const submit = document.getElementById('register-submit');
    const error = document.getElementById('register-error');
    const phonePattern = /^(?:\+91)?[6-9]\d{9}$/;
    const upiPattern = /^[A-Za-z0-9._-]+@[A-Za-z][A-Za-z0-9.-]+$/;

    // Theme selector cards logic
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

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        FeeKeeperApi.clearFieldErrors(form);
        error.hidden = true;
        const payload = values();
        const invalid = validate(payload);
        if (invalid) { showLocalError(invalid[0], invalid[1]); return; }
        submit.disabled = true;
        submit.textContent = 'Creating account…';
        try {
            const response = await FeeKeeperApi.request('/api/auth/register', {method: 'POST', auth: false, body: payload});
            FeeKeeperApi.saveToken(response.accessToken);
            window.location.replace('/dashboard');
        } catch (apiError) {
            FeeKeeperApi.showFieldErrors(form, apiError.problem && apiError.problem.fieldErrors);
            error.textContent = apiError.message;
            error.hidden = false;
            error.classList.remove('hidden');
        } finally {
            submit.disabled = false;
            submit.textContent = 'Create account';
        }
    });

    function values() {
        const checkedTheme = form.querySelector('input[name="receiptTheme"]:checked');
        return {
            businessName: form.businessName.value.trim(), ownerName: form.ownerName.value.trim(),
            phone: form.phone.value.trim(), upiId: form.upiId.value.trim() || null,
            email: form.email.value.trim(), password: form.password.value,
            receiptTheme: checkedTheme ? checkedTheme.value : 'ANDROMEDA'
        };
    }
    function validate(value) {
        if (!value.businessName) return ['businessName', 'Business name is required.'];
        if (!value.ownerName) return ['ownerName', 'Owner name is required.'];
        if (!phonePattern.test(value.phone)) return ['phone', 'Enter a valid Indian mobile number.'];
        if (value.upiId && !upiPattern.test(value.upiId)) return ['upiId', 'Enter a valid UPI ID such as name@bank.'];
        if (!form.email.validity.valid) return ['email', 'Enter a valid email address.'];
        if (value.password.length < 8 || value.password.length > 72) return ['password', 'Password must be 8 to 72 characters.'];
        return null;
    }
    function showLocalError(name, text) {
        const node = form.querySelector('[data-error-for="' + name + '"]');
        node.textContent = text;
        node.hidden = false;
        node.classList.remove('hidden');
        form.elements.namedItem(name).setAttribute('aria-invalid', 'true');
        form.elements.namedItem(name).focus();
    }
});
