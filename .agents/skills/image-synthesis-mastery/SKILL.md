---
name: image-synthesis-mastery
description: Expert prompt engineering and composition director for AI image synthesis (Agnes Image / DALL-E / Imagen / Stable Diffusion). Master multi-panel manga/comic layouts, explicit quoted typography, speech bubble rendering, cinematic lighting, and aspect ratio choreography.
---

# Image Synthesis & Typography Mastery Skill

This skill governs how prompts are engineered and expanded when generating images, illustrations, manga/comics, diagrams, and visual mockups. It eliminates deformed typography, disorganized layouts, and generic AI visual tropes.

---

## 1. Explicit Quoted Typography (Text-in-Image)

Modern image generation models (like DALL-E 3, Imagen 3, Agnes Image, and Flux) can render sharp, legible text ONLY when text is given with strict syntactic boundaries and typographic directives:

1. **Strict Double Quotes Requirement**:
   - Every single word, headline, speech bubble, or sign MUST be enclosed in explicit English double quotes: `"..."`.
   - Specify font style and weight: `bold sans-serif lettering`, `crisp black ink typeface`, `clean modern headline font`.
   - *Good*: `A speech bubble on the upper-right corner containing crisp, bold, clearly legible Indonesian text reading: "EL NINO MELANDA INDONESIA" in solid black sans-serif font with high contrast white background.`
   - *Bad*: `manga with el nino text in bubbles` (will result in garbled, mutated pseudo-letters).

2. **Negative Typographic Directives**:
   - Always append typographic quality anchors: `sharp vectorized typography, perfectly spelled words, zero gibberish letters, zero misspelled words, high contrast legibility`.

3. **News Banners & Signage**:
   - For television broadcasts, news lower-thirds, or street signs:
     `a crisp lower-third breaking news banner reading: "LIVE: KEBAKARAN HUTAN MELUAS", sharp white capitalized typography on dark semi-translucent red banner bar`.

---

## 2. Multi-Panel Manga & Comic Strip Choreography

When requested to produce a **manga, comic, manhwa, or sequential graphic**:

1. **Structural Panel Grid**:
   - Define exact panel count and hierarchy:
     `Structured multi-panel manga page with 4 distinct rectangular panels separated by clean white gutters (panel borders):`
     - **Panel 1 (Top Wide)**: Establishing shot with environment, lighting, and headline banner.
     - **Panel 2 (Middle Left)**: Dynamic action shot, character interaction, water hoses, fire.
     - **Panel 3 (Middle Right)**: Emotional character close-up, sweat, determination, smoke reflection in eyes.
     - **Panel 4 (Bottom Wide)**: Contrast scene or thematic climax with speech bubbles.
2. **Reading Flow & Speech Bubbles**:
   - Explicitly position speech bubbles (`upper-left`, `center-top`, `bottom-right`) so they do not obstruct character faces.
   - Anchor dialogue: `speech bubble with tail pointing to firefighter reading: "TETAP SEMANGAT!"`.
3. **Manga Visual Language**:
   - `Japanese manga anime aesthetic, detailed ink screentone hatching, dynamic speed lines, cel-shaded color grade, dramatic smoke and embers, cinematic chiaroscuro contrast`.

---

## 3. Mode Differentiation: Standard vs MAX Mode

1. **Standard Mode**:
   - Direct, high-quality prompt refinement focusing on subject, style, lighting, and composition (~50-80 words).
2. **MAX Mode (Claude/Agnes MAX Mode)**:
   - **Two-Stage Comprehensive Visual Director**:
     1. Analyze narrative depth, contrast, emotional weight, and panel division.
     2. Expand prompt into a master-grade 120-180 word visual blueprint detailing:
        - Exact camera lenses (e.g. `wide-angle 24mm lens for landscape`, `85mm portrait lens for close-up`).
        - Lighting temperature and volumetric effects (`harsh golden-orange wildfire illumination contrasting with cool interior chandelier light`).
        - Exact Indonesian/English quotes in speech bubbles.
        - Optimal Aspect Ratio selection:
          - Manga/Comics/Infographics: `9:16` (`1024x1792`) or `3:4` (`768x1024`).
          - Landscapes/Wallpapers/Cinematic: `16:9` (`1792x1024`).
          - Character Avatars/Logos: `1:1` (`1024x1024`).

---

## 4. Single Visual Output Mandate

- In image generation mode, the agent's primary product is the **Image itself**.
- Never dump raw prompt repetitions, verbose markdown summaries, or duplicate tool results after generating the image.
- Produce a clean single visual presentation.
