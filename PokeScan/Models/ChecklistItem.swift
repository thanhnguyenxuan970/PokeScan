import Foundation

struct ChecklistItem: Identifiable {
    let id: String
    let title: String
    let description: String
    let category: String

    static let standard: [ChecklistItem] = [
        ChecklistItem(
            id: "font_weight",
            title: "Check font weight",
            description: "Counterfeit HP/damage numbers are often thinner or bolder than genuine prints. Compare against a known real card.",
            category: "print_quality"
        ),
        ChecklistItem(
            id: "holo_pattern",
            title: "Inspect holo pattern",
            description: "Authentic holos have a consistent starburst or cosmos pattern. Fakes show repeating tiles, smearing, or flat silver fill.",
            category: "print_quality"
        ),
        ChecklistItem(
            id: "card_thickness",
            title: "Feel card thickness",
            description: "Genuine cards are 0.32mm thick. Fakes are often thinner (flex easily) or thicker (stiff, cardboard-like).",
            category: "physical"
        ),
        ChecklistItem(
            id: "set_symbol",
            title: "Verify set symbol",
            description: "The set symbol should be crisp and correctly sized. Blurry, offset, or missing symbols indicate a counterfeit.",
            category: "print_quality"
        ),
        ChecklistItem(
            id: "copyright_text",
            title: "Read copyright line",
            description: "Bottom text should read \"\u{00A9}[year] Pokémon/Nintendo/Creatures/GAME FREAK.\" Misspellings or wrong year are red flags.",
            category: "text"
        ),
        ChecklistItem(
            id: "back_rosette",
            title: "Examine card back",
            description: "The Pokéball back has a fine rosette print pattern. Fakes show solid color blocks under magnification.",
            category: "physical"
        ),
    ]
}
