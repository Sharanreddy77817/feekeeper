document.addEventListener('DOMContentLoaded', function () {
    'use strict';
    if (!FeeKeeperApi.requireSession()) return;

    const state = {page: 0, size: 10, totalPages: 0, search: '', status: '', members: new Map(), payment: null, currency: 'INR', currencySymbol: '₹'};
    const memberDialog = document.getElementById('member-dialog');
    const memberForm = document.getElementById('member-form');
    const paymentDialog = document.getElementById('payment-dialog');
    const paymentForm = document.getElementById('payment-form');
    const csvFileInput = document.getElementById('member-csv-file');

    document.getElementById('add-member').addEventListener('click', function () { openMemberDialog(); });
    document.getElementById('import-members').addEventListener('click', function () { csvFileInput.click(); });
    csvFileInput.addEventListener('change', importMembersCsv);
    document.getElementById('download-csv-template').addEventListener('click', downloadCsvTemplate);
    document.getElementById('retry-summary').addEventListener('click', loadSummary);
    document.getElementById('retry-members').addEventListener('click', loadMembers);
    document.getElementById('previous-page').addEventListener('click', function () { if (state.page > 0) { state.page--; loadMembers(); } });
    document.getElementById('next-page').addEventListener('click', function () { if (state.page + 1 < state.totalPages) { state.page++; loadMembers(); } });
    document.getElementById('status-filter').addEventListener('change', function (event) { state.status = event.target.value; state.page = 0; loadMembers(); });
    document.getElementById('member-search').addEventListener('input', debounce(function (event) { state.search = event.target.value.trim(); state.page = 0; loadMembers(); }, 350));
    document.querySelectorAll('[data-summary-status]').forEach(function (button) {
        button.addEventListener('click', function () {
            state.status = button.dataset.summaryStatus;
            document.getElementById('status-filter').value = state.status;
            state.page = 0;
            loadMembers();
            document.getElementById('members-heading').scrollIntoView({behavior: 'smooth'});
        });
    });
    document.querySelectorAll('[data-close-dialog]').forEach(function (button) {
        button.addEventListener('click', function () { document.getElementById(button.dataset.closeDialog).close(); });
    });
    document.getElementById('members-table').addEventListener('click', handleMemberAction);
    document.getElementById('members-mobile').addEventListener('click', handleMemberAction);
    memberForm.addEventListener('submit', saveMember);
    paymentForm.addEventListener('submit', markPaid);
    document.getElementById('download-receipt').addEventListener('click', function () { receivePdf(false); });
    document.getElementById('print-receipt').addEventListener('click', function () { receivePdf(true); });
    document.getElementById('preview-promotion').addEventListener('click', previewPromotion);

    FeeKeeperApi.request('/api/tenant/settings').then(function(s){state.currency=s.currencyCode||'INR';state.currencySymbol=s.currencySymbol||'₹';loadSummary();loadMembers();}).catch(function(){loadSummary();loadMembers();});

    async function loadSummary() {
        show('summary-loading'); hide('summary-cards'); hide('summary-error');
        try {
            const summary = await FeeKeeperApi.request('/api/dashboard');
            document.getElementById('active-count').textContent = String(summary.totalActiveMembers);
            document.getElementById('expiring-count').textContent = String(summary.expiringIn5Days);
            document.getElementById('overdue-count').textContent = String(summary.overdueMembers);
            document.getElementById('revenue-total').textContent = money(summary.revenueThisMonth);
            hide('summary-loading'); show('summary-cards');
        } catch (error) {
            if (error.status !== 401) { hide('summary-loading'); show('summary-error'); }
        }
    }

    async function loadMembers() {
        show('members-loading'); hide('members-error'); hide('members-empty'); hide('members-content');
        const params = new URLSearchParams({page: String(state.page), size: String(state.size), sort: 'fullName,asc'});
        if (state.search) params.set('search', state.search);
        if (state.status) params.set('status', state.status);
        try {
            const result = await FeeKeeperApi.request('/api/members?' + params.toString());
            state.totalPages = result.totalPages;
            state.members = new Map(result.content.map(function (member) { return [member.id, member]; }));
            hide('members-loading');
            if (!result.content.length) { show('members-empty'); return; }
            renderMembers(result.content);
            document.getElementById('page-label').textContent = 'Page ' + (state.page + 1) + ' of ' + Math.max(1, result.totalPages);
            document.getElementById('previous-page').disabled = result.first;
            document.getElementById('next-page').disabled = result.last;
            show('members-content');
        } catch (error) {
            if (error.status !== 401) { hide('members-loading'); show('members-error'); }
        }
    }

    function renderMembers(members) {
        const table = document.getElementById('members-table');
        const mobile = document.getElementById('members-mobile');
        table.replaceChildren(); mobile.replaceChildren();
        members.forEach(function (member) {
            const row = node('tr', 'hover:bg-slate-50');
            const identity = node('td', 'px-5 py-4');
            identity.append(node('p', 'font-bold text-slate-900', member.fullName), node('p', 'mt-0.5 text-xs text-slate-500', member.phone));
            const plan = node('td', 'px-5 py-4'); plan.append(node('p', 'font-semibold', member.planName), node('p', 'text-xs text-slate-500', money(member.planAmount) + ' · ' + member.planDurationDays + ' days'));
            const expiry = node('td', 'px-5 py-4 text-slate-700', formatDate(member.expiryDate));
            const status = node('td', 'px-5 py-4'); status.append(statusBadge(member.status));
            const actions = node('td', 'px-5 py-4');
            const actionWrap = node('div', 'flex justify-end gap-2');
            actionWrap.append(actionButton('pay', member.id, 'Mark paid', 'bg-indigo-600 text-white'), actionButton('whatsapp', member.id, 'WhatsApp', 'border border-slate-300'), actionButton('edit', member.id, 'Edit', 'border border-slate-300'), actionButton('delete', member.id, 'Delete', 'text-red-700'));
            actions.append(actionWrap); row.append(identity, plan, expiry, status, actions); table.append(row);

            const card = node('article', 'p-4');
            const top = node('div', 'flex items-start justify-between gap-3');
            const name = node('div'); name.append(node('h3', 'font-black', member.fullName), node('p', 'text-sm text-slate-500', member.phone));
            top.append(name, statusBadge(member.status));
            const details = node('dl', 'mt-4 grid grid-cols-2 gap-3 text-sm');
            details.append(detail('Plan', member.planName), detail('Amount', money(member.planAmount)), detail('Expiry', formatDate(member.expiryDate)), detail('Duration', member.planDurationDays + ' days'));
            const cardActions = node('div', 'mt-4 grid grid-cols-2 gap-2');
            cardActions.append(actionButton('pay', member.id, 'Mark paid', 'bg-indigo-600 text-white'), actionButton('whatsapp', member.id, 'WhatsApp', 'border border-slate-300'), actionButton('edit', member.id, 'Edit', 'border border-slate-300'), actionButton('delete', member.id, 'Delete', 'text-red-700'));
            card.append(top, details, cardActions); mobile.append(card);
        });
    }

    function handleMemberAction(event) {
        const button = event.target.closest('[data-action]');
        if (!button) return;
        const member = state.members.get(button.dataset.id);
        if (!member) return;
        if (button.dataset.action === 'edit') openMemberDialog(member);
        if (button.dataset.action === 'delete') deleteMember(member);
        if (button.dataset.action === 'pay') openPaymentDialog(member);
        if (button.dataset.action === 'whatsapp') openWhatsApp(member);
    }

    function openMemberDialog(member) {
        memberForm.reset(); FeeKeeperApi.clearFieldErrors(memberForm); hide('member-form-error');
        document.getElementById('member-id').value = member ? member.id : '';
        document.getElementById('member-dialog-title').textContent = member ? 'Edit member' : 'Add member';
        if (member) {
            memberForm.fullName.value = member.fullName; memberForm.phone.value = member.phone;
            memberForm.planName.value = member.planName; memberForm.planAmount.value = member.planAmount;
            memberForm.planDurationDays.value = String(member.planDurationDays); memberForm.joinDate.value = member.joinDate;
        } else {
            memberForm.planDurationDays.value = '30'; memberForm.joinDate.value = localToday();
        }
        memberDialog.showModal(); memberForm.fullName.focus();
    }

    async function saveMember(event) {
        event.preventDefault(); FeeKeeperApi.clearFieldErrors(memberForm); hide('member-form-error');
        const payload = {fullName: memberForm.fullName.value.trim(), phone: memberForm.phone.value.trim(), planName: memberForm.planName.value.trim(), planAmount: Number(memberForm.planAmount.value), planDurationDays: Number(memberForm.planDurationDays.value), joinDate: memberForm.joinDate.value};
        const invalid = validateMember(payload); if (invalid) { localFieldError(memberForm, invalid[0], invalid[1]); return; }
        const id = document.getElementById('member-id').value;
        const button = document.getElementById('save-member'); button.disabled = true; button.textContent = 'Saving…';
        try {
            await FeeKeeperApi.request(id ? '/api/members/' + id : '/api/members', {method: id ? 'PUT' : 'POST', body: payload});
            memberDialog.close(); FeeKeeperApi.showToast(id ? 'Member updated.' : 'Member added.');
            state.page = 0; await Promise.all([loadMembers(), loadSummary()]);
        } catch (error) {
            FeeKeeperApi.showFieldErrors(memberForm, error.problem && error.problem.fieldErrors);
            const box = document.getElementById('member-form-error'); box.textContent = error.message; box.hidden = false; box.classList.remove('hidden');
        } finally { button.disabled = false; button.textContent = 'Save member'; }
    }

    async function importMembersCsv() {
        const file = csvFileInput.files && csvFileInput.files[0];
        if (!file) return;
        const resultBox = document.getElementById('csv-import-result');
        const summary = document.getElementById('csv-import-summary');
        const errorsList = document.getElementById('csv-import-errors');
        const importButton = document.getElementById('import-members');
        errorsList.replaceChildren();
        summary.textContent = '';
        resultBox.classList.remove('hidden');
        resultBox.hidden = false;
        if (!file.name.toLowerCase().endsWith('.csv')) {
            summary.textContent = 'Choose a .csv file.';
            resultBox.classList.add('border-red-200', 'bg-red-50');
            csvFileInput.value = '';
            return;
        }
        const formData = new FormData();
        formData.append('file', file);
        importButton.disabled = true;
        importButton.textContent = 'Importing…';
        try {
            const result = await FeeKeeperApi.request('/api/members/import', {method: 'POST', body: formData});
            summary.textContent = 'Imported ' + result.importedCount + ' of ' + result.totalRows + ' rows. ' + result.failedCount + ' failed.';
            (result.errors || []).forEach(function (error) {
                errorsList.append(node('li', 'mt-1', 'Row ' + error.rowNumber + ' — ' + error.field + ': ' + error.message));
            });
            resultBox.classList.remove('border-red-200', 'bg-red-50');
            resultBox.classList.add(result.failedCount ? 'border-amber-200' : 'border-green-200', result.failedCount ? 'bg-amber-50' : 'bg-green-50');
            if (result.importedCount > 0) {
                state.page = 0;
                await Promise.all([loadMembers(), loadSummary()]);
            }
            FeeKeeperApi.showToast(result.importedCount + ' member(s) imported.');
        } catch (error) {
            summary.textContent = error.message || 'CSV import failed. Check the file and try again.';
            resultBox.classList.remove('border-amber-200', 'bg-amber-50', 'border-green-200', 'bg-green-50');
            resultBox.classList.add('border-red-200', 'bg-red-50');
            FeeKeeperApi.showToast('CSV import failed.', 'error');
        } finally {
            importButton.disabled = false;
            importButton.textContent = 'Import CSV';
            csvFileInput.value = '';
        }
    }

    function downloadCsvTemplate(event) {
        event.preventDefault();
        const csv = ['fullName,phone,planName,planAmount,planDurationDays,joinDate',
            'Ravi Kumar,9876543210,Monthly,700.00,30,' + localToday()].join('\r\n') + '\r\n';
        const url = URL.createObjectURL(new Blob([csv], {type: 'text/csv;charset=utf-8'}));
        const link = document.createElement('a');
        link.href = url;
        link.download = 'feekeeper-members-template.csv';
        document.body.append(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    }

    async function deleteMember(member) {
        if (!window.confirm('Delete ' + member.fullName + '? This hides the member and cannot be undone here.')) return;
        try { await FeeKeeperApi.request('/api/members/' + member.id, {method: 'DELETE'}); FeeKeeperApi.showToast('Member deleted.'); await Promise.all([loadMembers(), loadSummary()]); }
        catch (error) { FeeKeeperApi.showToast(error.message, 'error'); }
    }

    async function openWhatsApp(member) {
        const popup = window.open('about:blank', '_blank');
        try {
            const result = await FeeKeeperApi.request('/api/members/' + member.id + '/whatsapp-link');
            if (popup) popup.location.replace(result.url); else window.location.href = result.url;
        } catch (error) { if (popup) popup.close(); FeeKeeperApi.showToast(error.message, 'error'); }
    }

    function openPaymentDialog(member) {
        state.payment = {member: member, idempotencyKey: crypto.randomUUID(), result: null};
        paymentForm.reset(); paymentForm.hidden = false; show('confirm-payment'); hide('payment-success'); hide('payment-error');
        document.getElementById('payment-member-name').textContent = member.fullName + ' · ' + member.planName;
        document.getElementById('payment-amount').value = Number(member.planAmount).toFixed(2);
        document.getElementById('promotion-code').value=''; document.getElementById('promotion-preview').textContent=''; state.payment.promotionCode=null;
        paymentDialog.showModal(); document.getElementById('payment-amount').focus();
    }

    async function markPaid(event) {
        event.preventDefault(); hide('payment-error'); FeeKeeperApi.clearFieldErrors(paymentForm);
        const amount = Number(document.getElementById('payment-amount').value);
        if (!(amount > 0)) { localFieldError(paymentForm, 'amountPaid', 'Enter an amount greater than zero.'); return; }
        const mode = paymentForm.querySelector('[name="paymentMode"]:checked').value;
        const button = document.getElementById('confirm-payment'); button.disabled = true; button.textContent = 'Recording payment…';
        try {
            const result = await FeeKeeperApi.request('/api/payments/mark-paid/' + state.payment.member.id, {method: 'POST', headers: {'Idempotency-Key': state.payment.idempotencyKey}, body: {amountPaid: amount, paymentMode: mode, promotionCode: state.payment.promotionCode||null}});
            state.payment.result = result; state.payment.member.expiryDate = result.newExpiryDate; state.payment.member.status = 'ACTIVE';
            renderMembers(Array.from(state.members.values())); loadSummary();
            paymentForm.hidden = true; show('payment-success');
            document.getElementById('payment-success-detail').textContent = result.receiptNumber + ' · New expiry ' + formatDate(result.newExpiryDate);
            document.getElementById('share-receipt').href = receiptWhatsAppUrl(state.payment.member, result);
            FeeKeeperApi.showToast('Payment recorded successfully.');
        } catch (error) {
            const box = document.getElementById('payment-error'); box.textContent = error.message; box.hidden = false; box.classList.remove('hidden');
        } finally { button.disabled = false; button.textContent = 'Confirm payment'; }
    }

    async function receivePdf(print) {
        if (!state.payment || !state.payment.result) return;
        const printWindow = print ? window.open('about:blank', '_blank') : null;
        try {
            const blob = await FeeKeeperApi.request('/api/payments/' + state.payment.result.id + '/receipt.pdf', {responseType: 'blob'});
            const url = URL.createObjectURL(blob);
            if (print) {
                if (printWindow) {
                    printWindow.location.replace(url);
                    printWindow.addEventListener('load', function () { printWindow.print(); }, {once: true});
                } else FeeKeeperApi.showToast('Allow pop-ups to print the receipt.', 'error');
            } else {
                const link = document.createElement('a'); link.href = url; link.download = state.payment.result.receiptNumber + '.pdf'; document.body.append(link); link.click(); link.remove();
            }
            window.setTimeout(function () { URL.revokeObjectURL(url); }, 60000);
        } catch (error) { if (printWindow) printWindow.close(); FeeKeeperApi.showToast(error.message, 'error'); }
    }

    function receiptWhatsAppUrl(member, result) {
        let digits = member.phone.replace(/\D/g, ''); if (digits.length === 10) digits = '91' + digits;
        const text = 'Payment received by FeeKeeper. Receipt ' + result.receiptNumber + ', amount ' + money(result.amountPaid) + ', new expiry ' + formatDate(result.newExpiryDate) + '.';
        return 'https://wa.me/' + digits + '?text=' + encodeURIComponent(text);
    }

    function validateMember(value) {
        if (!value.fullName) return ['fullName', 'Full name is required.'];
        if (!/^(?:\+91)?[6-9]\d{9}$/.test(value.phone)) return ['phone', 'Enter a valid Indian mobile number.'];
        if (!value.planName) return ['planName', 'Plan name is required.'];
        if (!(value.planAmount > 0)) return ['planAmount', 'Amount must be greater than zero.'];
        if (![30, 90, 180, 365].includes(value.planDurationDays)) return ['planDurationDays', 'Choose 30, 90, 180, or 365 days.'];
        if (!value.joinDate) return ['joinDate', 'Join date is required.'];
        return null;
    }
    function localFieldError(form, name, text) { const message = form.querySelector('[data-error-for="' + name + '"]'); message.textContent = text; message.hidden = false; message.classList.remove('hidden'); form.elements.namedItem(name).focus(); }
    function actionButton(action, id, label, classes) { const button = node('button', 'focus-ring rounded-lg px-3 py-2 text-xs font-bold ' + classes, label); button.type = 'button'; button.dataset.action = action; button.dataset.id = id; return button; }
    function statusBadge(status) { const labels = {ACTIVE: 'Active', EXPIRING_SOON: 'Expiring soon', OVERDUE: 'Overdue'}; return node('span', 'inline-flex rounded-full px-2.5 py-1 text-xs font-bold status-' + status.toLowerCase().replace('_', '-'), labels[status]); }
    function detail(label, value) { const wrap = node('div'); wrap.append(node('dt', 'text-xs uppercase tracking-wide text-slate-500', label), node('dd', 'mt-0.5 font-semibold', value)); return wrap; }
    function node(tag, classes, text) { const element = document.createElement(tag); if (classes) element.className = classes; if (text !== undefined) element.textContent = text; return element; }
    function formatDate(value) { return new Intl.DateTimeFormat('en-IN', {day: '2-digit', month: 'short', year: 'numeric'}).format(new Date(value + 'T00:00:00')); }
    function money(value) { return state.currencySymbol + new Intl.NumberFormat('en', {minimumFractionDigits:2,maximumFractionDigits:2}).format(Number(value || 0)) + ' ' + state.currency; }
    async function previewPromotion(){const code=document.getElementById('promotion-code').value.trim();const preview=document.getElementById('promotion-preview');if(!code){preview.textContent='Enter a promotion code.';return;}try{const amount=Number(document.getElementById('payment-amount').value);const result=await FeeKeeperApi.request('/api/promotions/validate',{method:'POST',body:{code:code,amount:amount,planDurationDays:state.payment.member.planDurationDays}});state.payment.promotionCode=code;preview.textContent='Discount: '+money(result.calculatedDiscount)+' · Pay '+money(Math.max(0,amount-result.calculatedDiscount));}catch(e){state.payment.promotionCode=null;preview.textContent=e.message;}}
    function localToday() { const now = new Date(); now.setMinutes(now.getMinutes() - now.getTimezoneOffset()); return now.toISOString().slice(0, 10); }
    function debounce(callback, wait) { let timer; return function (event) { window.clearTimeout(timer); const target = event.target; timer = window.setTimeout(function () { callback({target: target}); }, wait); }; }
    function show(id) { const element = document.getElementById(id); element.classList.remove('hidden'); element.hidden = false; }
    function hide(id) { document.getElementById(id).hidden = true; }
});
