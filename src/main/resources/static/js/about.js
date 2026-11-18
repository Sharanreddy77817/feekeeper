(() => {
    'use strict';

    const pricingData = {
        USD: {
            code: 'USD',
            symbol: '$',
            monthly: '49',
            festival: '39',
            festivalTag: '20% Holiday Discount',
            yearly: '390',
            yearlyPerMonth: '$32.50 / mo',
            yearlySavings: 'Save $198/year (2 Months Free)'
        },
        EUR: {
            code: 'EUR',
            symbol: '€',
            monthly: '45',
            festival: '36',
            festivalTag: '20% Seasonal Promo',
            yearly: '360',
            yearlyPerMonth: '€30.00 / mo',
            yearlySavings: 'Save €180/year (2 Months Free)'
        },
        GBP: {
            code: 'GBP',
            symbol: '£',
            monthly: '39',
            festival: '31',
            festivalTag: '20% Summer Boost',
            yearly: '310',
            yearlyPerMonth: '£25.80 / mo',
            yearlySavings: 'Save £158/year (2 Months Free)'
        },
        AED: {
            code: 'AED',
            symbol: 'AED ',
            monthly: '180',
            festival: '145',
            festivalTag: '20% Ramadan / Eid Special',
            yearly: '1,440',
            yearlyPerMonth: 'AED 120 / mo',
            yearlySavings: 'Save AED 720/year'
        },
        INR: {
            code: 'INR',
            symbol: '₹',
            monthly: '1,999',
            festival: '1,499',
            festivalTag: 'Diwali & New Year Offer: 25% OFF',
            yearly: '14,999',
            yearlyPerMonth: '₹1,249 / mo',
            yearlySavings: 'Save ₹8,989/year (Pay 10, Get 12 Months)'
        }
    };

    let currentCurrency = 'USD';
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
