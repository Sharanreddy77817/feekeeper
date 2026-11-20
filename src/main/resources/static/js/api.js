(function () {
    'use strict';
    const TOKEN_KEY = 'feekeeper.jwt';
    const THEME_KEY = 'feekeeper.theme';

    function setupTheme() {
        let saved = null;
        try { saved = localStorage.getItem(THEME_KEY); } catch (ignored) { /* Use this page's default theme when storage is unavailable. */ }
        const initialTheme = saved || (document.body.classList.contains('bg-slate-900') ? 'dark' : 'light');
        document.documentElement.dataset.theme = initialTheme;

        const button = document.createElement('button');
        button.type = 'button';
        button.id = 'theme-toggle';
        button.className = 'theme-toggle focus-ring';
        const navigation = document.querySelector('nav > div');
        const publicActions = document.querySelector('header > div > div:last-child');
        if (navigation) {
            button.classList.add('theme-toggle-inline');
            navigation.appendChild(button);
        }
        else if (publicActions) {
            button.classList.add('theme-toggle-inline');
            publicActions.appendChild(button);
        } else button.classList.add('theme-toggle-corner');
        button.setAttribute('aria-pressed', String(initialTheme === 'dark'));
        button.addEventListener('click', function () {
            const nextTheme = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
            document.documentElement.dataset.theme = nextTheme;
            try { localStorage.setItem(THEME_KEY, nextTheme); } catch (ignored) { /* Theme remains active for this page. */ }
            button.setAttribute('aria-pressed', String(nextTheme === 'dark'));
            updateThemeButton(button, nextTheme);
        });
        updateThemeButton(button, initialTheme);
        if (!navigation && !publicActions) document.body.appendChild(button);
    }

    function updateThemeButton(button, theme) {
        button.textContent = theme === 'dark' ? '☀' : '☾';
        button.setAttribute('aria-label', theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode');
        button.title = button.getAttribute('aria-label');
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', setupTheme, {once: true});
    else setupTheme();

    class ApiError extends Error {
        constructor(problem, status) {
            super(problem && problem.message ? problem.message : 'Something went wrong. Please try again.');
            this.name = 'ApiError';
            this.status = status;
            this.problem = problem || {};
        }
    }

    async function request(path, options) {
        const config = Object.assign({method: 'GET', auth: true}, options || {});
        const headers = new Headers(config.headers || {});
        if (config.auth) {
            const token = sessionStorage.getItem(TOKEN_KEY);
            if (token) headers.set('Authorization', 'Bearer ' + token);
        }
        if (config.body !== undefined && !(config.body instanceof FormData)) {
            headers.set('Content-Type', 'application/json');
            config.body = JSON.stringify(config.body);
        }
        let response;
        try {
            response = await fetch(path, {method: config.method, headers: headers, body: config.body});
        } catch (ignored) {
            throw new ApiError({message: 'Unable to reach FeeKeeper. Check your connection and try again.'}, 0);
        }
        if (response.status === 401 && config.auth) {
            sessionStorage.removeItem(TOKEN_KEY);
            window.location.replace('/login');
            throw new ApiError({message: 'Your session has expired.'}, 401);
        }
        if (!response.ok) {
            let problem = {};
            try { problem = await response.json(); } catch (ignored) { problem = {message: 'Request failed.'}; }
            throw new ApiError(problem, response.status);
        }
        if (config.responseType === 'blob') return response.blob();
        if (response.status === 204) return null;
        return response.json();
    }

    function requireSession() {
        if (!sessionStorage.getItem(TOKEN_KEY)) {
            window.location.replace('/login');
            return false;
        }
        return true;
    }

    function showToast(message, type) {
        const toast = document.getElementById('toast');
        if (!toast) return;
        toast.textContent = message;
        toast.className = 'pointer-events-none fixed inset-x-4 top-4 z-[80] mx-auto max-w-md rounded-xl px-4 py-3 text-sm font-semibold text-white shadow-lg ' +
            (type === 'error' ? 'bg-red-700' : 'bg-slate-900');
        toast.hidden = false;
        window.clearTimeout(showToast.timer);
        showToast.timer = window.setTimeout(function () { toast.hidden = true; }, 3500);
    }

    function clearFieldErrors(form) {
        form.querySelectorAll('[data-error-for]').forEach(function (node) {
            node.textContent = '';
            node.hidden = true;
        });
        form.querySelectorAll('[aria-invalid="true"]').forEach(function (node) { node.removeAttribute('aria-invalid'); });
    }

    function showFieldErrors(form, fields) {
        (fields || []).forEach(function (violation) {
            const message = form.querySelector('[data-error-for="' + CSS.escape(violation.field) + '"]');
            const input = form.elements.namedItem(violation.field);
            if (message) { message.textContent = violation.message; message.hidden = false; message.classList.remove('hidden'); }
            if (input) input.setAttribute('aria-invalid', 'true');
        });
    }

    function logout() {
        sessionStorage.removeItem(TOKEN_KEY);
        window.location.replace('/login');
    }

    window.FeeKeeperApi = {
        request: request,
        ApiError: ApiError,
        token: function () { return sessionStorage.getItem(TOKEN_KEY); },
        saveToken: function (token) { sessionStorage.setItem(TOKEN_KEY, token); },
        requireSession: requireSession,
        showToast: showToast,
        clearFieldErrors: clearFieldErrors,
        showFieldErrors: showFieldErrors,
        logout: logout
    };
}());
