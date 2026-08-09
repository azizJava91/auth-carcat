"""PO-facing PDF: PIN lock behaviour (short, colorful)."""
from pathlib import Path

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
OUT = DOCS / "pin_lock_behaviour.pdf"
FONT = Path(r"C:\Windows\Fonts\arial.ttf")
FONT_BOLD = Path(r"C:\Windows\Fonts\arialbd.ttf")


class Pdf(FPDF):
    def __init__(self):
        super().__init__()
        self.add_font("T", "", str(FONT))
        self.add_font("T", "B", str(FONT_BOLD if FONT_BOLD.exists() else FONT))
        self._f = "T"

    def header(self):
        self.set_fill_color(37, 99, 235)
        self.rect(0, 0, 210, 22, "F")
        self.set_y(6)
        self.set_font(self._f, "B", 11)
        self.set_text_color(255, 255, 255)
        self.cell(0, 8, "CarCat Auth  ·  PIN lock behaviour", align="C")
        self.ln(20)

    def footer(self):
        self.set_y(-14)
        self.set_font(self._f, "", 8)
        self.set_text_color(140, 140, 140)
        self.cell(0, 10, f"Internal · Page {self.page_no()}", align="C")

    def h1(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "B", 18)
        self.set_text_color(15, 23, 42)
        self.multi_cell(self.epw, 9, t)
        self.ln(2)

    def lead(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 11)
        self.set_text_color(71, 85, 105)
        self.multi_cell(self.epw, 6, t)
        self.ln(3)

    def section(self, title, color):
        self.ln(2)
        self.set_x(self.l_margin)
        self.set_fill_color(*color)
        self.set_font(self._f, "B", 11)
        self.set_text_color(255, 255, 255)
        self.cell(self.epw, 8, f"  {title}", fill=True)
        self.ln(10)

    def bullet(self, t):
        self.set_x(self.l_margin)
        self.set_font(self._f, "", 10)
        self.set_text_color(30, 41, 59)
        self.multi_cell(self.epw, 5.5, f"  •  {t}")

    def badge_row(self, code, label, bg, fg=(255, 255, 255)):
        self.set_x(self.l_margin)
        self.set_fill_color(*bg)
        self.set_font(self._f, "B", 10)
        self.set_text_color(*fg)
        self.cell(22, 7, code, align="C", fill=True)
        self.set_fill_color(248, 250, 252)
        self.set_text_color(30, 41, 59)
        self.set_font(self._f, "", 10)
        self.cell(self.epw - 22, 7, f"  {label}", fill=True)
        self.ln(9)

    def card(self, title, lines, border_rgb):
        self.set_x(self.l_margin)
        y0 = self.get_y()
        self.set_draw_color(*border_rgb)
        self.set_fill_color(255, 255, 255)
        self.set_line_width(0.6)
        # estimate height
        h = 8 + len(lines) * 5.5 + 6
        self.rect(self.l_margin, y0, self.epw, h, "D")
        self.set_xy(self.l_margin + 3, y0 + 2)
        self.set_font(self._f, "B", 10)
        self.set_text_color(*border_rgb)
        self.multi_cell(self.epw - 6, 5, title)
        self.set_font(self._f, "", 9.5)
        self.set_text_color(51, 65, 85)
        for line in lines:
            self.set_x(self.l_margin + 3)
            self.multi_cell(self.epw - 6, 5, f"• {line}")
        self.set_y(y0 + h + 3)


def build():
    pdf = Pdf()
    pdf.set_margins(16, 16, 16)
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.add_page()

    pdf.h1("PIN lock — how login protection works")
    pdf.lead(
        "Short product note for PO: after repeated wrong PINs, the account is "
        "temporarily locked. Flutter can show a countdown from the API response."
    )

    pdf.section("Rules (agreed)", (37, 99, 235))
    pdf.bullet("User-based lock (not device / IP).")
    pdf.bullet("Wrong PIN counter lives in a 10-minute window.")
    pdf.bullet("3 wrong PINs inside that window → lock for 6 minutes.")
    pdf.bullet("While locked: even a correct PIN is rejected (option A).")
    pdf.bullet("Successful login clears the counter and any lock.")
    pdf.bullet("If the next wrong PIN comes after 10+ minutes, counter restarts at 1.")

    pdf.section("What the user / app sees", (16, 185, 129))
    pdf.badge_row("200", "Correct PIN, not locked → login success (tokens)", (16, 185, 129))
    pdf.badge_row("401", "Wrong PIN (1st or 2nd in the window)", (239, 68, 68))
    pdf.badge_row("423", "Account temporarily locked — show countdown", (217, 119, 6))

    pdf.section("423 response (for Flutter countdown)", (217, 119, 6))
    pdf.card(
        "Body fields",
        [
            'error: "PIN_LOCKED"',
            "message: localized text (AZ / EN / RU)",
            "lockedUntil: timestamp when unlock happens",
            "remainingSeconds: seconds left (use for timer UI)",
            "status: 423",
        ],
        (217, 119, 6),
    )

    pdf.section("Simple timeline example", (99, 102, 241))
    pdf.card(
        "Scenario",
        [
            "12:00 wrong → 401 (attempt 1)",
            "12:02 wrong → 401 (attempt 2)",
            "12:03 wrong → 423 locked until 12:09",
            "12:05 correct PIN → still 423 (must wait)",
            "12:09+ correct PIN → 200 login success",
            "OR: 12:00 wrong, then 12:15 wrong → counter = 1 again (window expired)",
        ],
        (99, 102, 241),
    )

    pdf.section("Config knobs", (100, 116, 139))
    pdf.bullet("max attempts: 3")
    pdf.bullet("attempt window: 10 minutes")
    pdf.bullet("lock duration: 6 minutes")
    pdf.bullet("status field stays ACTIVE — temporary lock uses separate timestamps")

    pdf.ln(4)
    pdf.set_fill_color(239, 246, 255)
    pdf.set_x(pdf.l_margin)
    pdf.set_font(pdf._f, "", 9)
    pdf.set_text_color(30, 64, 175)
    pdf.multi_cell(
        pdf.epw,
        5,
        "  Endpoint: POST /api/v1/users/login   ·   No change to public URL. "
        "Only login behaviour + error payload for lock.",
        fill=True,
    )

    pdf.output(str(OUT))
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
