(() => {
    'use strict';

    const pricingData = {
        USD: {
            code: 'USD',
            symbol: '$',
            monthly: '19',
            festival: '19',
            festivalTag: '1 month free trial',
            yearly: '190',
            yearlyPerMonth: '$15.83 / mo, billed yearly',
            yearlySavings: 'Save $38/year'
        },
        EUR: {
            code: 'EUR',
            symbol: '€',
            monthly: '17', festival: '17', festivalTag: '1 month free trial', yearly: '170',
            yearlyPerMonth: '€14.17 / mo, billed yearly', yearlySavings: 'Save €34/year'
        },
        GBP: {
            code: 'GBP',
            symbol: '£',
            monthly: '15', festival: '15', festivalTag: '1 month free trial', yearly: '150',
            yearlyPerMonth: '£12.50 / mo, billed yearly', yearlySavings: 'Save £30/year'
        },
        AED: {
            code: 'AED',
            symbol: 'AED ',
            monthly: '69', festival: '69', festivalTag: '1 month free trial', yearly: '690',
            yearlyPerMonth: 'AED 57.50 / mo, billed yearly', yearlySavings: 'Save AED 138/year'
        },
        AUD: { code: 'AUD', symbol: 'A$', monthly: '29', festival: '29', festivalTag: '1 month free trial', yearly: '290', yearlyPerMonth: 'A$24.17 / mo, billed yearly', yearlySavings: 'Save A$58/year' },
        CAD: { code: 'CAD', symbol: 'C$', monthly: '26', festival: '26', festivalTag: '1 month free trial', yearly: '260', yearlyPerMonth: 'C$21.67 / mo, billed yearly', yearlySavings: 'Save C$52/year' },
        INR: {
            code: 'INR',
            symbol: '₹',
            monthly: '999', festival: '999', festivalTag: '1 month free trial', yearly: '9,990',
            yearlyPerMonth: '₹833 / mo, billed yearly', yearlySavings: 'Save ₹1,998/year'
        }
    };

    let currentCurrency = 'INR';
    let currentMode = 'yearly'; // 'monthly', 'festival', 'yearly'

    function updateDisplay() {
        const cur = pricingData[currentCurrency] || pricingData.USD;
        
        // Update currency button active states
        document.querySelectorAll('[data-currency-btn]').forEach(btn => {
            const code = btn.getAttribute('data-currency-btn');
            if (code === currentCurrency) {
                btn.className = 'px-3 py-1.5 rounded-lg text-sm font-bold bg-indigo-600 text-white shadow-md transition-all scale-105';
            } else {
                btn.className = 'px-3 py-1.5 rounded-lg text-sm font-medium bg-slate-100 text-slate-700 hover:bg-slate-200 transition-all';
            }
        });

        // Update plan cards
        const priceEls = {
            monthly: document.getElementById('price-monthly'),
            festival: document.getElementById('price-festival'),
            yearly: document.getElementById('price-yearly'),
            festivalTag: document.getElementById('tag-festival'),
            yearlySavings: document.getElementById('tag-yearly-savings'),
            yearlySub: document.getElementById('sub-yearly'),
            demoCurrencyTag: document.getElementById('demo-currency-label')
        };

        if (priceEls.monthly) priceEls.monthly.textContent = `${cur.symbol}${cur.monthly}`;
        if (priceEls.festival) priceEls.festival.textContent = `${cur.symbol}${cur.festival}`;
        if (priceEls.yearly) priceEls.yearly.textContent = `${cur.symbol}${cur.yearly}`;
        if (priceEls.festivalTag) priceEls.festivalTag.textContent = cur.festivalTag;
        if (priceEls.yearlySavings) priceEls.yearlySavings.textContent = cur.yearlySavings;
        if (priceEls.yearlySub) priceEls.yearlySub.textContent = cur.yearlyPerMonth;
        if (priceEls.demoCurrencyTag) priceEls.demoCurrencyTag.textContent = `${cur.code} (${cur.symbol})`;
    }

    document.addEventListener('DOMContentLoaded', () => {
        const countrySelect = document.getElementById('country-select');
        if (countrySelect) {
            const detected = (navigator.language || '').toUpperCase().split('-').pop();
            const countryCurrency = { IN: 'INR', US: 'USD', GB: 'GBP', IE: 'EUR', DE: 'EUR', FR: 'EUR', ES: 'EUR', IT: 'EUR', NL: 'EUR', AE: 'AED' };
            const preferred = countryCurrency[detected] || 'INR';
            if ([...countrySelect.options].some(option => option.value === preferred)) countrySelect.value = preferred;
            currentCurrency = countrySelect.value;
            countrySelect.addEventListener('change', () => {
                currentCurrency = countrySelect.value;
                updateDisplay();
            });
        }
        document.querySelectorAll('[data-currency-btn]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const target = e.currentTarget.getAttribute('data-currency-btn');
                if (target && pricingData[target]) {
                    currentCurrency = target;
                    updateDisplay();
                }
            });
        });

        updateDisplay();
    });
})();
