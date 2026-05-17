CHECKLIST_ITEMS = [
    {
        "id": "font_weight",
        "title": "Check font weight",
        "description": (
            "Counterfeit HP/damage numbers are often thinner or bolder than genuine prints. "
            "Compare against a known real card."
        ),
        "category": "print_quality",
    },
    {
        "id": "holo_pattern",
        "title": "Inspect holo pattern",
        "description": (
            "Authentic holos have a consistent starburst or cosmos pattern. "
            "Fakes show repeating tiles, smearing, or flat silver fill."
        ),
        "category": "print_quality",
    },
    {
        "id": "card_thickness",
        "title": "Feel card thickness",
        "description": (
            "Genuine cards are 0.32mm thick. "
            "Fakes are often thinner (flex easily) or thicker (stiff, cardboard-like)."
        ),
        "category": "physical",
    },
    {
        "id": "set_symbol",
        "title": "Verify set symbol",
        "description": (
            "The set symbol should be crisp and correctly sized. "
            "Blurry, offset, or missing symbols indicate a counterfeit."
        ),
        "category": "print_quality",
    },
    {
        "id": "copyright_text",
        "title": "Read copyright line",
        "description": (
            "©[year] Pokémon/Nintendo/Creatures/GAME FREAK."
            " Misspellings or wrong year are red flags."
        ),
        "category": "text",
    },
    {
        "id": "back_rosette",
        "title": "Examine card back",
        "description": (
            "The Pokéball back has a fine rosette print pattern. "
            "Fakes show solid color blocks under magnification."
        ),
        "category": "physical",
    },
]


def get_inspection_checklist() -> list[dict]:
    return CHECKLIST_ITEMS
