# Meta WhatsApp Cloud API setup

FeeKeeper already includes a direct Meta Cloud API sender. This guide covers the Meta account and production configuration needed to turn it on. Keep `WHATSAPP_PROVIDER=mock` until the steps below are complete; once enabled, the daily job can send reminders to every eligible tenant member.

## 1. Verify the Zovak business in Meta

1. In Meta Business Suite, open Business Settings / Security Center and start business verification for the correct business portfolio.
2. Enter the legal business name, address, website, and contact information so they match the submitted records.
3. If Meta offers Udyam/MSME registration as an accepted document type, submit the certificate through Meta's verification screen. Do not send the certificate or Udyam number in chat, email, or source control.
4. Complete any confirmation Meta requests by email, phone, or other accepted method. A document upload does not itself complete verification.

Meta's verification flow is the authority on which documents and proof are accepted for this portfolio. Keep the certificate locally until that screen specifically requests it.

## 2. Add WhatsApp and register the sender

1. In Meta for Developers, create or select the Zovak app and add the WhatsApp product.
2. Associate the intended WhatsApp Business Account and sending phone number. Complete the number verification and display-name review requested by Meta.
3. Check whether the sender number is currently attached to WhatsApp Business on a phone. Do not remove or migrate it until Meta shows the supported coexistence/migration option and the owner understands any effect on the existing app. A wrong registration can interrupt the current WhatsApp service.
4. Copy the **Phone Number ID** from the WhatsApp API setup screen. FeeKeeper needs this ID, not the visible phone number and not the WhatsApp Business Account ID.

## 3. Create and approve the reminder template

Create one **Utility** template with name `fee_reminder`, locale `en_IN`, and a single text body component. Use this body so the six placeholders match FeeKeeper's sender exactly:

```text
Hello {{1}}, this is a friendly reminder from {{2}}. {{3}} Amount due: ₹{{4}}. Expiry date: {{5}}. UPI ID: {{6}}. Thank you.
```

Provide realistic sample values for all six placeholders in Meta's examples field. The parameter order is member name, business name, reminder text, amount, expiry date, and UPI ID/payment instructions. Submit it and wait until Meta marks it **Approved**. If Meta requires a different locale or changes the category, use the exact approved values in the app configuration.

## 4. Create a production access token

Create a System User token in the correct business portfolio and assign it access to the WhatsApp app and sending number. Grant only the WhatsApp permissions needed by the app, including `whatsapp_business_messaging`; use `whatsapp_business_management` if Meta requires it for managing/assigning the business assets. Do not use the short-lived token shown for initial testing in production.

Keep the token private. Put it directly into the Render environment variable `WHATSAPP_API_KEY`; never paste it into chat, source files, screenshots, or logs.

## 5. Configure Render

In the FeeKeeper Render service, add/update:

| Variable | Value |
| --- | --- |
| `WHATSAPP_PROVIDER` | `meta` |
| `WHATSAPP_BASE_URL` | `https://graph.facebook.com/v27.0` |
| `WHATSAPP_API_KEY` | Production System User access token; mark secret |
| `WHATSAPP_PHONE_NUMBER_ID` | Phone Number ID from Meta |
| `WHATSAPP_TEMPLATE_NAME` | `fee_reminder` |
| `WHATSAPP_LANGUAGE_CODE` | `en_IN`, or the exact locale Meta approved |

Save the environment changes and wait for Render to redeploy. The API version must be reviewed during periodic Meta API upgrades.

## 6. Controlled first send

Before the first automated run, confirm the template is Approved, the sender status is Connected, the token and Phone Number ID belong to the same business assets, the intended recipients opted in to reminders, and at least one eligible member is a controlled test recipient. The scheduler runs daily at 10:00 Asia/Kolkata and sends to all eligible tenants; it is not a single-recipient test switch. Monitor Render logs by request/member ID only; FeeKeeper intentionally does not log tokens or full phone numbers.

Meta documents the Cloud API message endpoint as `/{PHONE_NUMBER_ID}/messages`, the template approval lifecycle, and the required phone-number registration separately in its [Cloud API overview](https://developers.facebook.com/documentation/business-messaging/whatsapp/overview/), [template API reference](https://developers.facebook.com/docs/graph-api/reference/whats-app-business-account/message_templates/), and [phone registration reference](https://developers.facebook.com/docs/graph-api/reference/whats-app-business-account-to-number-current-status/register/).
