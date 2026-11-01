# Closet Architecture & Component Rules (Regla de Oro)

This document establishes the foundational architectural rules governing all closet containers, interior partitions, accessories, and modular components in **ClosetStudio 3D**.

---

## 1. Unified Selection Protocol (The 3-Action Floating Menu)
Every component within or associated with a closet carcass (the carcass itself, vertical dividers, horizontal shelves, drawer banks, clothes rails, pant racks, etc.) **must strictly follow this interaction contract**:

1. **Tap / Selection in 3D Viewport:**
   - Highlights the selected component with the designated selection outline (e.g., Purple `#8B5CF6`).
   - Displays **only** the 3 floating action buttons on the screen:
     - ✏️ **Edit (`Editar`)**: Opens the contextual lateral inspector panel ("Module") for fine-grained dimension and coordinate editing.
     - ❐ **Duplicate (`Copiar / Duplicar`)**: Duplicates the component within the active or adjacent compartment.
     - 🗑️ **Delete (`Eliminar`)**: Removes the component from the carcass.
   - **Rule:** Selecting an element **must never** automatically open the lateral inspector panel. The panel only opens when the user explicitly taps the ✏️ **Edit** button.
   - Tapping an empty area or another container deselects the element and closes the action buttons.

---

## 2. Modular Compartment Architecture (Cell / Compartment System)
Interior space partitioning follows a strict compartment-based hierarchy:

1. **Interior Volume Representation:**
   - The interior volume of any closet container is defined as $[0, W_{\text{inner}}] \times [0, H_{\text{inner}}] \times [0, D_{\text{inner}}]$.
   - Partitions divide space into distinct, non-overlapping 3D compartments (cells).

2. **Compartment-Bounded Placement & Adaptation:**
   - Components are assigned to a specific compartment $[X_{\min}, X_{\max}] \times [Y_{\min}, Y_{\max}]$.
   - **Horizontal Shelves:** Bound between surrounding vertical panels or carcass side panels:
     $$\text{Width} = X_{\max} - X_{\min}$$
   - **Vertical Dividers:** Do not need to be full-height (floor-to-ceiling). If a horizontal shelf divides a bay into upper and lower sections, moving or adding a vertical divider into that area bounds it between the shelf and floor/top:
     $$\text{Height} = Y_{\max} - Y_{\min}$$
     The vertical divider adapts to the available height of the target cell rather than cutting through the existing shelf.

3. **Adjacent Adaptation:**
   - Repositioning a divider or shelf automatically recalibrates the dimensions of all dependent components residing within affected compartments.

---

## 3. Depth Independence with Strict Back-Anchor
Every interior component has an independently configurable depth (`depthMm`):

1. **Fixed Posterior Anchor:**
   - The rear face of every interior partition and accessory is anchored against the carcass back panel ($Z = 0$ inner).
   - **Invariant:** A component **can never penetrate backward** through the back panel or host wall ($Z_{\min} \ge 0$).

2. **Anterior Freedom:**
   - If $\text{depthMm} < \text{carcassDepth}$, the component recedes inward, leaving space at the front.
   - If $\text{depthMm} > \text{carcassDepth}$, the component projects outward from the front of the carcass into the room.

---

## 4. Direct 3D Gesture Repositioning (Drag & Slide)
1. **Interactive Manipulation:**
   - When a component is selected, dragging it directly in the 3D viewport slides it within its valid compartment bounds.
   - **Vertical Dividers:** Dragging horizontally shifts position along the compartment's $X$ axis, stopping at the boundaries of adjacent dividers or carcass sides.
   - **Horizontal Shelves:** Dragging vertically adjusts elevation along the $Y$ axis; dragging horizontally across bay thresholds transfers the shelf into adjacent compartments, adapting its width automatically.

---

## 5. Strict Carpentry Containment Invariant (Zero-Penetration Rule / Regla de Oro de Armado de Melamina)
This rule replicates physical cabinetry assembly where melamina boards must never collide with or penetrate through the carcass structural panels:

1. **Absolute Structural Envelope:**
   The carcass panels (top roof panel, bottom floor panel, left side panel, and right side panel) establish the impenetrable outer shell of the container:
   - **Vertical Interior Space:**
     $$Y \in [Y_{\text{floor, top}}, Y_{\text{roof, bottom}}] = [\text{plinthHeight} + t, \text{totalHeight} - t]$$
     $$\text{Interior Height} = \text{totalHeight} - \text{plinthHeight} - 2t$$
   - **Horizontal Interior Space:**
     $$X \in [X_{\text{left, inner}}, X_{\text{right, inner}}] = [s_0 + t, s_1 - t]$$
     $$\text{Interior Width} = \text{totalWidth} - 2t$$
   where $t = \text{effectivePanelThicknessMm}$ and $\text{plinthHeight} = \text{effectivePlinthHeightMm}$.

2. **Zero-Penetration Guarantee:**
   - **Under NO circumstances** can any internal component (vertical dividers, horizontal shelves, future drawers, clothes rails, pant racks, lighting strips, or any accessory) penetrate, protrude into, or cut through the carcass roof, floor, or side panels.
   - **Vertical Dividers:** Must rest flush on the top face of the bottom floor panel ($Y_{\min} = \text{plinthHeight} + t$) and terminate strictly flush against the bottom face of the top roof panel ($Y_{\max} = \text{totalHeight} - t$) or under an intervening horizontal shelf. They can never penetrate above $Y_{\text{roof, bottom}}$ or below $Y_{\text{floor, top}}$.
   - **Horizontal Shelves:** Must remain bounded within $[Y_{\text{floor, top}}, Y_{\text{roof, bottom}} - \text{thickness}]$ and span strictly between adjacent vertical dividers or carcass sides $[X_{\min}, X_{\max}]$.
   - **Selection Outlines & Visual Highlights:** Visual outlines and selection rims are clamped to the interior volume $[Y_{\text{floor, top}}, Y_{\text{roof, bottom}}]$ so that halos and highlight boxes never pierce or bleed into carcass boards.

3. **Single Source of Truth:**
   The carcass panel thickness $t$ (`effectivePanelThicknessMm`) and plinth height (`effectivePlinthHeightMm`) must remain mathematically identical across all subsystems: Core Model (`Project.kt`), 3D Geometry Generation (`RoomGeometry.kt`), Raycasting/Tap Picking, and Gesture Handlers (`RoomScreen.kt`). Clamping is strictly enforced at both the domain model and 3D mesh generator levels.

---

## 6. Catalog Drag-and-Drop Insertion Protocol (Regla de Oro de Inserción y Arrastre en Vivo)
**Golden Rule for all current and future components:** When inserting any component (vertical dividers, horizontal shelves, drawer banks, shoe racks, clothes rails, accessories) from the "Add Unit" catalog into a closet container, this interaction contract must be strictly followed:

1. **Immediate In-Container Live 3D Placement:**
   - As soon as the user drags a component from the catalog over the closet container, the piece is immediately inserted into the 3D scene inside the carcass cavity.
   - The piece moves live in 3D under the user's finger, adapting dynamically in real time to the hovered compartment (sub-bay width, shelf interval height) before release. The user sees exactly where and how it sits prior to dropping.

2. **Clean Drop & Auto-Close (No Automatic Inspector):**
   - **Upon Release (Drop):** The component finalizes at the dropped position and remains selected (displaying the 3 floating action buttons: ✏️ Edit, ❐ Duplicate, 🗑️ Delete).
   - **Auto-Close Catalog:** The "Add Unit" catalog popup closes automatically upon drop.
   - **No Inspector Modal:** The lateral dimension inspector panel ("Module") **must NEVER** open automatically upon drop. It opens exclusively when the user explicitly taps the ✏️ Edit floating button.

---

## 7. Smart Magnetic Alignment & Snapping Protocol (Regla de Oro de Alineado Inteligente)
Manual millimetric alignment of cabinetry partitions on touchscreen devices is physically frustrating. To guarantee precision carpentry and effortless layout creation, all partition repositioning (both horizontal shelves and vertical dividers) **must adhere to smart magnetic snapping**:

1. **Horizontal Shelf Alignment (Continuidad Horizontal entre Módulos):**
   - **Cross-Bay Shelf Snap (Priority 1):** When moving or dragging a horizontal shelf in any compartment or bay, its elevation $Y$ automatically detects all other shelves across the closet container. When the shelf enters a magnetic tolerance zone ($\Delta Y \le 28\text{ mm}$), it magnetically snaps ("jale magnético") to the exact elevation of the reference shelf. This eliminates millimetric guessing and creates seamless horizontal continuity across vertical dividers.
   - **Structural Midpoints & Thirds (Priority 2):** When no matching shelf is nearby, the shelf magnetically snaps to the container vertical midpoint $(H_{\text{inner}} - t) \times 0.5$ (creating two equal-height clear compartments), or equal thirds/quarters.
   - **Visual Confirmation:** When aligned, both the active shelf and the matching reference shelf display the purple selection highlight outline simultaneously in the 3D viewport, giving instant visual confirmation of the alignment.

2. **Vertical Divider Alignment (Alineación y Simetría Vertical):**
   - **Column Continuity (Priority 1):** When dragging a partial vertical divider (bounded between shelves), it magnetically snaps to the horizontal offset $X$ of existing vertical dividers in compartments above or below, maintaining continuous vertical mullions.
   - **Center & Symmetrical Divisions (Priority 2):** Vertical dividers snap to the container center $(W_{\text{inner}} - t) \times 0.5$ (splitting the carcass into two equal bays), thirds (three equal bays), or the center of any individual bay being bisected.

3. **Smooth Magnetic Hysteresis:**
   - Magnetic capture threshold is set to $28\text{ mm}$ (approx. $10\text{--}15\text{ px}$ in standard 3D projection).
   - While within $\pm 28\text{ mm}$, the position locks to the alignment target.
   - Moving the finger beyond the $28\text{ mm}$ threshold smoothly releases the lock back to free continuous movement without jarring teleportation.

---

## 9. Clothes Rail & Hanging Rod Protocol (Regla de Oro de Tubos Colgadores)
The clothes rail (tubo o fierro colgador) replicates wardrobe hanging rods for garments and coat hangers:

1. **Default Centered Placement:**
   - **Vertical Center:** When added to a closet compartment, elevation $Y$ is automatically centered between the compartment boundaries (floor, shelves, or roof):
     $$Y = \frac{\text{comp}Y_0 + \text{comp}Y_1}{2}$$
   - **Depth Center:** By default, the rail is centered along the interior depth:
     $$Z = D_{\text{inner}} \times 0.5$$
     providing symmetrical clearance for hanger shoulders towards both the back wall and the front opening.

2. **Structural Anatomy & Realistic Hardware:**
   - **Chrome Rail Extrusion:** Smooth cylindrical extrusion along the horizontal bay axis ($S$-axis) with polished chrome reflection (`RoomSurface.DOOR_HANDLE`).
   - **Mounting Flange Brackets:** Dual lateral circular mounting brackets ($8\text{ mm}$ thick) pressed flush against the interior melamina panels on both sides.
   - **Configurable Diameter:** Default wardrobe standard $25\text{ mm}$, adjustable in the inspector from $15\text{ mm}$ to $50\text{ mm}$.

3. **Zero Penetration & Smart Magnetic Alignment:**
   - Elevation is clamped strictly within inner bounds $[r, H_{\text{inner}} - r]$ where $r = \frac{\text{diameter}}{2}$.
   - **Cross-Bay Rail Snap:** When dragging a clothes rail vertically, it snaps to the elevation of matching rails in other bays ($\pm 28\text{ mm}$ tolerance) to easily align continuous hanging sections.

4. **Unified Interaction Contract:**
   - Dragging from the catalog into the closet cavity displays live 3D insertion feedback under the finger.
   - Dropping finalizes placement, auto-closes the catalog popup, leaves the rail selected with purple highlight, and **does not open the inspector modal automatically**.
   - Standard 3 floating action buttons: ✏️ Edit (inspect dimensions, diameter, elevation, and depth offset), ❐ Duplicate, 🗑️ Delete.

---

## 10. Vertical Pole & Support Post Protocol (Regla de Oro de Tubos Verticales)
The vertical pole / post (tubo vertical o columna/poste de soporte metálico) replicates structural vertical chrome poles spanning floor-to-shelf, shelf-to-shelf, or floor-to-roof:

1. **Default Placement & Compartment Containment:**
   - **Horizontal Center:** When added to a closet compartment or bay, horizontal position $X$ defaults to the center of the target bay:
     $$X = \frac{\text{bay.startXMm} + \text{bay.endXMm}}{2}$$
   - **Vertical Span:** Spans between compartment vertical bounds:
     $$Y \in [\text{comp}Y_0, \text{comp}Y_1]$$
     If placed in an undivided bay, it spans floor-to-roof; if placed above or below a shelf, it automatically bounds between the shelf and floor/roof.
   - **Depth Center:** Centered along the interior depth:
     $$Z = D_{\text{inner}} \times 0.5$$

2. **Structural Anatomy & Realistic Hardware:**
   - **Chrome Cylinder Extrusion:** Smooth 12-sided vertical cylinder extrusion along the $Y$-axis with polished chrome reflection (`RoomSurface.DOOR_HANDLE`).
   - **Mounting Flange Brackets:** Top and bottom circular flange brackets ($8\text{ mm}$ thick) with circular extrusion and flat caps, flush against the melamina surface.
   - **Configurable Diameter:** Default wardrobe standard $25\text{ mm}$, adjustable in the inspector from $15\text{ mm}$ to $50\text{ mm}$.

3. **Zero Penetration & Lateral Movement:**
   - Position $X$ is clamped strictly within bay bounds $[X_{\min} + r, X_{\max} - r]$ where $r = \frac{\text{diameter}}{2}$.
   - **Horizontal Sliding:** Dragging horizontally shifts position within its bay or transfers into adjacent bays.
   - **Smart Magnetic Snapping:** Snaps to bay center, bay thirds, and aligns with other vertical poles or dividers.

4. **Unified Interaction Contract:**
   - Dragging from catalog into the closet cavity displays live 3D insertion feedback under the finger.
   - Dropping finalizes placement, auto-closes catalog, leaves pole selected with purple highlight, and **never opens inspector automatically**.
   - Standard 3 floating action buttons: ✏️ Edit (inspect bay width, height, diameter, depth, left/right clearances), ❐ Duplicate, 🗑️ Delete.

---

## 11. Inclined Shoe Rack Protocol (Regla de Oro de Zapateras Inclinadas)
The inclined shoe rack (zapatera inclinada) provides ergonomic tilted footwear shelves bounded within a modular closet bay:

1. **Inclined Shelf Geometry & Solid Wood Retaining Lip:**
   - **Slanted Board Extrusion:** Spans the entire clear width of its containing bay ($S \in [s_0, s_1]$). Tilts downward from back to front by configurable inclination angle $\theta$ (default $18^\circ$, adjustable from $5^\circ$ to $35^\circ$):
     $$\Delta Y = D_{\text{span}} \times \tan(\theta)$$
   - **Solid Wood Front Retaining Lip (Zócalo / tope frontal de madera sólida):** Replaces any metal wire mesh with a solid rectangular upright melamina lip spanning the front edge ($S \in [s_0, s_1]$, $D \in [d_1 - t_{\text{lip}}, d_1]$) rising above the shelf surface (default height $40\text{ mm}$, adjustable from $20\text{ mm}$ to $80\text{ mm}$; thickness $18\text{ mm}$).
   - **Zero Wire Mesh:** Strictly constructed with cabinetry melamina matching the closet body (`RoomSurface.CONTAINER_BODY` and `CONTAINER_FRONT_EDGE`).

2. **Zero Penetration & Enclosing Envelope:**
   - Total vertical span is computed exactly:
     $$H_{\text{span}} = \max(\Delta Y + t_v, t_v + h_{\text{lip}})$$
     where $t_v = \frac{t}{\cos\theta}$.
   - Elevation $Y$ is clamped within inner bounds $[0, H_{\text{inner}} - H_{\text{span}}]$, guaranteeing zero penetration into the roof, floor, or adjacent panels.

3. **Smart Magnetic Alignment:**
   - **Cross-Bay Shoe Rack Snap:** Snaps coplanar to matching shoe racks in adjacent bays within $\pm 28\text{ mm}$ threshold to maintain visual rhythm.
   - **Vertical Stacking Snap:** Snaps vertically with ergonomic clearance ($H_{\text{span}} + 30\text{ mm}$) when placing multiple shoe racks in the same bay.
   - **Floor Snap:** Easily aligns to $Y = 0\text{ mm}$.

4. **Unified Interaction Contract:**
   - **Visual Catalog 3D Graphic:** 8th unit in catalog with dedicated Canvas illustration `ShoeRack3DGraphic` showing the inclined wood slab and front solid wood lip.
   - **Live 3D Drag & Drop:** Real-time preview inside closet bay during drag.
   - **Silent Placement:** Dropping finalizes placement, closes catalog, leaves shoe rack selected with purple highlight, and **does not open the inspector modal automatically**.
   - **Floating Action Buttons:** ✏️ Edit, ❐ Duplicate (stacks above with clearance), 🗑️ Delete.
   - **Badge:** Displays `"$baseLetter • ZAP"` when selected.
   - **Inspector:** Displays bay width (read-only), board thickness ($12-30\text{ mm}$), depth ($200-D_{\text{inner}}\text{ mm}$), tilt angle ($5^\circ-35^\circ$), front lip height ($20-80\text{ mm}$), and top/bottom clearances.







