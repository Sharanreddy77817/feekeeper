# Phase 4 manual test checklist

Use a 360px browser viewport once for every screen, then repeat the dashboard checks at desktop width. Keep the browser network panel open and confirm no JWT appears in a URL or console output.

## Account and session

- [ ] Open `/register`, submit blank/invalid fields, and confirm inline validation is readable and associated with each field.
- [ ] Register a business with valid Indian phone and optional `name@bank` UPI ID. Confirm redirect to `/dashboard` and that the JWT exists only in `sessionStorage`.
- [ ] Log out from `/settings`; confirm the token is removed and `/dashboard` redirects to `/login` when opened without it.
- [ ] Log in with an incorrect password and confirm only the generic “Email or password is incorrect” message appears.
- [ ] Log in correctly and confirm redirect to the dashboard.

## Members and dashboard

- [ ] Confirm summary loading indicators resolve into Active, Expiring Soon, Overdue, and monthly revenue cards.
- [ ] Add a 30-day member. Repeat for 90 and 180 days, and verify validation rejects other durations or invalid phone/amount values.
- [ ] Edit the member and confirm the row/card updates.
- [ ] Search by a partial name and partial phone. Clear search and filter using each summary card and the status selector.
- [ ] Verify desktop table and mobile stacked-card layouts, pagination buttons, empty results, and a simulated offline/error state.
- [ ] Open WhatsApp reminder and confirm a `wa.me` tab contains the expected prefilled member reminder.
- [ ] Delete a member, cancel once, then confirm deletion and verify the member disappears.

## Payment and receipt

- [ ] Open Mark Paid and confirm the plan amount is prefilled and editable, with UPI/Cash options.
- [ ] Double-click Confirm rapidly. Confirm only one payment/receipt is created, the button disables while pending, and the same idempotency key is reused if the request is retried before closing the modal.
- [ ] Confirm the member expiry/status and summary cards update without a browser reload.
- [ ] Download the authenticated PDF receipt and verify business, member, plan, amount, mode, date, receipt number, and new expiry.
- [ ] Print the receipt and open the WhatsApp receipt link.

## Settings and tenant isolation

- [ ] Load settings, reject an invalid UPI ID, save valid business/owner/phone/UPI changes, and confirm the success toast.
- [ ] Create tenant A and tenant B in separate browser profiles or an incognito window. Create a member under B and capture its ID from B’s member-create network response.
- [ ] While authenticated as A, request `/api/members/{B_MEMBER_ID}` with A’s bearer token using an API client. Confirm `404`, then verify B’s name/phone never appears in A’s search, dashboard totals, payments, or receipt requests.
