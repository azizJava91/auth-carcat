"""PO-facing Jira-style report: CRCT-181 QA findings and BE fixes."""
from pathlib import Path
from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "po_qa_fix_report_crct181.pdf"
FONT = Path(r"C:\Windows\Fonts\arial.ttf")
FONT_B = Path(r"C:\Windows\Fonts\arialbd.ttf")


class Pdf(FPDF):
    def __init__(self):
        super().__init__()
        self.add_font("T", "", str(FONT))
        self.add_font("T", "B", str(FONT_B if FONT_B.exists() else FONT))
        self.f = "T"

    def header(self):
        self.set_font(self.f, "", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 8, "CRCT-181 — BE fix report for Product / QA", align="R")
        self.ln(4)

    def footer(self):
        self.set_y(-14)
        self.set_font(self.f, "", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 10, f"Page {self.page_no()}", align="C")

    def h1(self, t):
        self.set_font(self.f, "B", 14)
        self.set_text_color(20, 20, 20)
        self.multi_cell(self.epw, 7, t)
        self.ln(2)

    def h2(self, t):
        self.ln(3)
        if self.get_y() > 255:
            self.add_page()
        self.set_font(self.f, "B", 11)
        self.set_text_color(25, 25, 25)
        self.multi_cell(self.epw, 6, t)
        self.ln(1)

    def label(self, title):
        self.set_font(self.f, "B", 9)
        self.set_text_color(50, 50, 50)
        self.multi_cell(self.epw, 4.5, title)

    def p(self, t):
        self.set_font(self.f, "", 9)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 4.5, t)
        self.ln(0.5)

    def bullet(self, t):
        self.set_font(self.f, "", 9)
        self.set_text_color(30, 30, 30)
        self.multi_cell(self.epw, 4.5, f"- {t}")

    def ticket(self, key, title, qa, fix, verify):
        self.h2(f"{key} — {title}")
        self.label("What QA saw")
        self.p(qa)
        self.label("What we changed")
        self.p(fix)
        self.label("How to re-check")
        self.p(verify)


def build():
    pdf = Pdf()
    pdf.set_margins(14, 14, 14)
    pdf.set_auto_page_break(auto=True, margin=16)
    pdf.add_page()

    pdf.h1("BE fix report — NewUsers auth (epic CRCT-181)")
    pdf.p(
        "Short status for Product: first NewUsers deploy was checked against the Jira tickets. "
        "Below is each ticket in the same style as the task descriptions — what was wrong, "
        "what backend fixed, and what QA should re-run. "
        "Legacy /api/v1/users stays for the old app. New flow is only /api/v1/newUsers."
    )

    pdf.h2("Scope reminder")
    pdf.bullet("Legacy Postman contract unchanged (register, OTP Bearer, setPassword, password field).")
    pdf.bullet("NewUsers final paths: /auth, /otp/createAndSend, /otp/verify, /setPinCode, /login.")
    pdf.bullet("purpose is only inside JWT. Server log: purpose extracted from JWT: <value>")
    pdf.bullet("password column = legacy BCrypt; pin_hash = NewUsers Argon2id.")
    pdf.bullet("setPinCode success = { status: PIN_SET } — no tokens. Client then calls login.")

    pdf.ticket(
        "CRCT-195",
        "Endpoint naming",
        "Paths still had *New suffix (createAndSendNew, verifyNew, loginNew) even under /newUsers.",
        "Suffix removed. Controllers and security permit lists use the short names only.",
        "Call the five NewUsers URLs without *New. Swagger NewUsers group should match.",
    )

    pdf.ticket(
        "CRCT-202",
        "Legacy setPassword must not use PIN rules",
        "PUT /users/setPassword rejected long passwords with Weak PIN / 4-digit rules (leak from NewUsers).",
        "Legacy setPassword / invite again accept free-form password and store BCrypt in password. "
        "PinValidator + Argon2id apply only to NewUsers setPinCode / login against pin_hash.",
        "Old Postman set password with a long password must succeed. NewUsers still rejects 1111 / 1234.",
    )

    pdf.ticket(
        "CRCT-182",
        "/auth behaviour",
        "Expectations around user creation, SMS, PIN presence, RESET unknown phone, and rate limits were not all met.",
        "/auth does not create user and does not send SMS. Returns authToken + next (SEND_OTP | PIN_CHECK). "
        "PIN present = pin_hash not null. RESET + no user / no PIN → treat as REGISTER (no 404), per PO. "
        "Phone normalized to +994… Invalid phone → 400. Rate limit 10/min phone, 30/min IP → 429.",
        "New phone → SEND_OTP. User with pin_hash → PIN_CHECK. RESET unknown phone → SEND_OTP + REGISTER, not 404.",
    )

    pdf.ticket(
        "CRCT-186",
        "createAndSend OTP",
        "Wrong next value, wrong token field names, wrong HTTP codes on bad/expired token, OTP leakage risk, language.",
        "Body uses authToken only. Success next = VERIFY_OTP. OTP hashed; never in JSON. "
        "Expired authToken → 401 AUTH_TOKEN_EXPIRED; garbage → 401 INVALID_TOKEN. "
        "30s cooldown; 3 sends / 15 min → phone lock ~5 min with countdown; IP abuse → 24h generic 429. "
        "SMS text follows Accept-Language (az / en / ru). Resend replaces previous pending code.",
        "Happy path next=VERIFY_OTP. Expired JWT → AUTH_TOKEN_EXPIRED. Switch Accept-Language and check SMS.",
    )

    pdf.ticket(
        "CRCT-187",
        "verify OTP",
        "Wrong codes / lock behaviour inconsistent; counters could roll back under the main transaction.",
        "Wrong code → 401 OTP_INCORRECT (counter in REQUIRES_NEW). 3 wrong → 429 OTP_VERIFY_LOCKED (~300s). "
        "Expired code → 400 OTP_EXPIRED. Success creates user if missing, next=SET_PIN, new authToken with purpose|SET_PIN. "
        "Clears send/verify counters on success.",
        "Three wrong OTPs → lock. Correct OTP after unlock → SET_PIN + new authToken.",
    )

    pdf.ticket(
        "CRCT-188",
        "setPinCode",
        "purpose query, wrong success payload, weak PIN codes, hashing / overwrite behaviour unclear.",
        "purpose read from JWT only (logged). Body: authToken + pinCode. Weak same-digit / sequences → 400 WEAK_PIN; "
        "wrong length → 400 PIN_LENGTH_ERROR. Stored Argon2id in pin_hash. Success: { status: PIN_SET }. "
        "Token single-use. RESET / overwrite revokes other refresh sessions; history rows kept.",
        "No purpose query. Weak PIN returns WEAK_PIN. Success body is only PIN_SET — then login separately.",
    )

    pdf.ticket(
        "CRCT-189",
        "login",
        "Wrong error codes, device field naming, refresh upsert vs history, lock persistence.",
        "Body: phoneNumber, pinCode, deviceId (deviceToken alias), platform optional. Compares pin_hash with Argon2id. "
        "Wrong → 401 PIN_INCORRECT; no pin_hash → 401 PIN_NOT_SET. 3 wrong → 429 LOGIN_LOCKED (~300s), survives reinstall. "
        "Refresh rows APPEND (audit history). deviceId stored on each new row.",
        "Wrong PIN → PIN_INCORRECT. Fresh user without PIN → PIN_NOT_SET. deviceId required. New refresh row each login.",
    )

    pdf.ticket(
        "CRCT-203",
        "IP / rate limiting",
        "Concern that locks looked like a permanent blacklist or were missing on /auth.",
        "OTP phone/IP locks are temporary DB rows (otp_rate_limits / ip_otp_rate_limits), not a permanent ban list. "
        "/auth uses a sliding window (phone 10/min, IP 30/min). Limits are configurable.",
        "Burst /auth → 429. Wait window → succeeds again. OTP IP abuse → message-only 429 without countdown.",
    )

    pdf.h2("Also done (cross-cutting)")
    pdf.bullet("Swagger OpenAPI NewUsers group updated to final path names and PIN_SET flow.")
    pdf.bullet("Desktop Postman collection: legacy folder untouched; NewUsers folder uses final paths + deviceId.")
    pdf.bullet("Flutter recommendation PDF regenerated with the same contract.")
    pdf.bullet("PIN lock counters use separate transactions so failed login/OTP attempts are not rolled back.")

    pdf.h2("Out of this BE delivery")
    pdf.bullet("CRCT-201 clean test data — ops after QA finishes (manual; not auto-wipe of real users).")
    pdf.bullet("Dropping password column — only after full PIN migration.")

    pdf.h2("Suggested QA package (re-open FAIL cases)")
    pdf.bullet("CRCT-195 — path names")
    pdf.bullet("CRCT-202 — legacy long password on setPassword")
    pdf.bullet("CRCT-186 — next=VERIFY_OTP; AUTH_TOKEN_EXPIRED / INVALID_TOKEN")
    pdf.bullet("CRCT-187 — OTP_INCORRECT + OTP_VERIFY_LOCKED")
    pdf.bullet("CRCT-188 — status PIN_SET; WEAK_PIN / PIN_LENGTH_ERROR; purpose from JWT")
    pdf.bullet("CRCT-189 — PIN_INCORRECT / PIN_NOT_SET / LOGIN_LOCKED + deviceId")
    pdf.bullet("CRCT-182 / CRCT-203 — /auth next rules + rate limits")

    pdf.ln(2)
    pdf.p(
        "Prepared for Product Owner / QA. Implementation lives in carland_auth "
        "(NewUsers package + legacy password isolation)."
    )

    pdf.output(OUT)
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
