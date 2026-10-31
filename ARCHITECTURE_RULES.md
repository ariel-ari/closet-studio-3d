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
