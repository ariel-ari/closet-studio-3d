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




