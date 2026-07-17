package com.dinomiha.dotmod.feature.invsee;

public record InvSeeLayout(Rect inventoryPanel, Rect catalogPanel, Rect status, Rect footer, boolean wide, boolean compact) {
    private static final int MARGIN = 4;
    private static final int HEADER_BOTTOM = 38;
    private static final int FOOTER_HEIGHT = 48;
    private static final int STATUS_HEIGHT = 22;
    private static final int INVENTORY_WIDTH = 216;
    private static final int CATALOG_WIDTH = 198;
    private static final int GAP = 8;

    public static InvSeeLayout compute(int screenWidth, int screenHeight, boolean catalogEnabled) {
        int footerY = Math.max(HEADER_BOTTOM + 88, screenHeight - FOOTER_HEIGHT - MARGIN);
        Rect footer = new Rect(MARGIN, footerY, Math.max(1, screenWidth - MARGIN * 2), Math.max(1, screenHeight - footerY - MARGIN));
        Rect status = new Rect(MARGIN, footerY - STATUS_HEIGHT, Math.max(1, screenWidth - MARGIN * 2), STATUS_HEIGHT);
        int contentHeight = Math.max(82, status.y() - HEADER_BOTTOM - GAP);
        boolean wide = catalogEnabled && screenWidth >= INVENTORY_WIDTH + CATALOG_WIDTH + GAP + MARGIN * 2;

        if (wide) {
            int totalWidth = INVENTORY_WIDTH + GAP + CATALOG_WIDTH;
            int startX = Math.max(MARGIN, (screenWidth - totalWidth) / 2);
            Rect inventory = new Rect(startX, HEADER_BOTTOM, INVENTORY_WIDTH, contentHeight);
            Rect catalog = new Rect(startX + INVENTORY_WIDTH + GAP, HEADER_BOTTOM, CATALOG_WIDTH, contentHeight);
            return new InvSeeLayout(inventory, catalog, status, footer, true, false);
        }

        int panelWidth = Math.min(INVENTORY_WIDTH, Math.max(1, screenWidth - MARGIN * 2));
        int startX = Math.max(MARGIN, (screenWidth - panelWidth) / 2);
        Rect panel = new Rect(startX, HEADER_BOTTOM, panelWidth, contentHeight);
        return new InvSeeLayout(panel, panel, status, footer, false, catalogEnabled);
    }

    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        public boolean inside(int screenWidth, int screenHeight) {
            return x >= 0 && y >= 0 && width > 0 && height > 0 && x + width <= screenWidth && y + height <= screenHeight;
        }
    }
}
