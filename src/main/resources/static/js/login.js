document.addEventListener('DOMContentLoaded', function () {
    'use strict';
    if (FeeKeeperApi.token()) { window.location.replace('/dashboard'); return; }
    const form = document.getElementById('login-form');
    const submit = document.getElementById('login-submit');
    const error = document.getElementById('login-error');

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        FeeKeeperApi.clearFieldErrors(form);
        error.hidden = true;
        const email = form.email.value.trim();
        const password = form.password.value;
        if (!email || !form.email.validity.valid) return fieldError('email', 'Enter a valid email address.');
        if (password.length < 8) return fieldError('password', 'Password must have at least 8 characters.');
        submit.disabled = true;
        submit.textContent = 'Signing in…';
        try {
            const response = await FeeKeeperApi.request('/api/auth/login', {
                method: 'POST', auth: false, body: {email: email, password: password}
            });
            FeeKeeperApi.saveToken(response.accessToken);
            window.location.replace('/dashboard');
        } catch (ignored) {
            error.textContent = 'Email or password is incorrect.';
            error.hidden = false;
            error.classList.remove('hidden');
        } finally {
            submit.disabled = false;
            submit.textContent = 'Sign in';
        }
    });

    function fieldError(name, text) {
        const node = form.querySelector('[data-error-for="' + name + '"]');
        node.textContent = text;
        node.hidden = false;
        node.classList.remove('hidden');
        form.elements.namedItem(name).setAttribute('aria-invalid', 'true');
        form.elements.namedItem(name).focus();
    }
});
