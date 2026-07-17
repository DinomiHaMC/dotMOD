package com.dinomiha.dotmod.feature.preset.screen;

import com.dinomiha.dotmod.feature.preset.helper.PresetHelperModel;
import com.dinomiha.dotmod.feature.preset.helper.PresetRequirement;
import com.dinomiha.dotmod.feature.preset.helper.RecipeAvailability;
import com.dinomiha.dotmod.feature.preset.helper.RecipeDependencyTree;
import com.dinomiha.dotmod.feature.preset.helper.RecipeOption;
import com.dinomiha.dotmod.feature.preset.helper.RequirementFilter;
import com.dinomiha.dotmod.ui.component.DotButton;
import com.dinomiha.dotmod.ui.component.DotTextField;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PresetHelperScreen extends Screen {
    private final Screen parent;
    private final PresetHelperModel model;
    private final List<PresetRequirementRowWidget> rows = new ArrayList<>();
    private List<PresetRequirement> filtered = List.of();
    private TextFieldWidget searchField;
    private ButtonWidget filterButton;
    private ButtonWidget recipeButton;
    private ButtonWidget listButton;
    private RequirementFilter filter = RequirementFilter.ALL;
    private PresetRequirement selected;
    private int recipeIndex;
    private int listFirst;
    private int detailFirstLine;
    private List<OrderedText> detailCache = List.of();
    private boolean detailsDirty = true;
    private String searchQuery = "";
    private boolean compactDetails;
    private int listWidth;
    private int listRows;

    public PresetHelperScreen(Screen parent, PresetHelperModel model) {
        super(Text.translatable("screen.dotmod.preset.helper.title", model.preset().preset().name()));
        this.parent = parent;
        this.model = model;
        this.selected = model.progress().requirements().stream().findFirst().orElse(null);
    }

    @Override
    protected void init() {
        rows.clear();
        boolean compact = width < 520;
        if (!compact) {
            compactDetails = false;
        }
        detailsDirty = true;
        listWidth = compact ? width - 16 : Math.min(260, width / 2 - 12);
        searchField = DotTextField.create(
                textRenderer, 8, 30, Math.max(50, listWidth - 104), 20,
                Text.translatable("screen.dotmod.preset.helper.search"),
                Text.translatable("screen.dotmod.preset.helper.search.placeholder"),
                searchQuery, 64, value -> true, value -> {
                    searchQuery = value;
                    rebuild();
                }
        );
        addDrawableChild(searchField);
        filterButton = addDrawableChild(DotButton.create(
                12 + Math.max(50, listWidth - 104), 30, 100,
                filterText(), button -> {
                    filter = filter.next();
                    filterButton.setMessage(filterText());
                    listFirst = 0;
                    rebuild();
                }
        ));
        listButton = addDrawableChild(DotButton.create(
                8, 30, 96, Text.translatable("screen.dotmod.preset.helper.list"), button -> {
                    compactDetails = false;
                    updateVisibility();
                }
        ));
        recipeButton = addDrawableChild(DotButton.create(
                compact ? 108 : listWidth + 16, 30, 150, Text.empty(), button -> nextRecipe()
        ));
        addDrawableChild(DotButton.create(
                Math.max(8, width - 88), height - 26, 80,
                Text.translatable("screen.dotmod.ism.close"), button -> close()
        ));

        listRows = Math.max(1, (height - 88) / 23);
        for (int index = 0; index < listRows; index++) {
            rows.add(addDrawableChild(new PresetRequirementRowWidget(
                    8, 56 + index * 23, listWidth, textRenderer, this::select
            )));
        }
        rebuild();
    }

    private void rebuild() {
        String query = searchQuery.strip().toLowerCase(Locale.ROOT);
        filtered = model.progress().requirements().stream()
                .filter(requirement -> matchesFilter(requirement))
                .filter(requirement -> query.isEmpty()
                        || requirement.stack().getName().getString().toLowerCase(Locale.ROOT).contains(query)
                        || Registries.ITEM.getId(requirement.stack().getItem()).toString().contains(query))
                .toList();
        listFirst = Math.max(0, Math.min(listFirst, Math.max(0, filtered.size() - listRows)));
        refreshRows();
        updateRecipeButton();
        updateVisibility();
    }

    private boolean matchesFilter(PresetRequirement requirement) {
        return switch (filter) {
            case ALL -> true;
            case MISSING -> !requirement.complete();
            case COMPLETE -> requirement.complete();
            case CRAFTABLE -> model.craftable(requirement);
        };
    }

    private void refreshRows() {
        for (int index = 0; index < rows.size(); index++) {
            int requirementIndex = listFirst + index;
            PresetRequirement requirement = requirementIndex < filtered.size() ? filtered.get(requirementIndex) : null;
            rows.get(index).setRequirement(
                    requirement, requirement == selected,
                    requirement != null && model.craftable(requirement)
            );
        }
    }

    private void select(PresetRequirement requirement) {
        selected = requirement;
        recipeIndex = 0;
        detailFirstLine = 0;
        detailsDirty = true;
        if (width < 520) {
            compactDetails = true;
        }
        refreshRows();
        updateRecipeButton();
        updateVisibility();
    }

    private void nextRecipe() {
        List<RecipeOption> recipes = selected == null ? List.of() : model.recipes(selected);
        if (!recipes.isEmpty()) {
            recipeIndex = (recipeIndex + 1) % recipes.size();
            detailFirstLine = 0;
            detailsDirty = true;
        }
        updateRecipeButton();
    }

    private void updateRecipeButton() {
        List<RecipeOption> recipes = selected == null ? List.of() : model.recipes(selected);
        recipeIndex = recipes.isEmpty() ? 0 : Math.min(recipeIndex, recipes.size() - 1);
        recipeButton.setMessage(recipes.isEmpty()
                ? Text.translatable("screen.dotmod.preset.helper.recipe.none")
                : Text.translatable("screen.dotmod.preset.helper.recipe", recipeIndex + 1, recipes.size()));
        recipeButton.active = recipes.size() > 1;
    }

    private void updateVisibility() {
        boolean compact = width < 520;
        boolean listVisible = !compact || !compactDetails;
        searchField.setVisible(listVisible);
        filterButton.visible = listVisible;
        rows.forEach(row -> row.visible = listVisible);
        listButton.visible = compact && compactDetails;
        recipeButton.visible = !compact || compactDetails;
    }

    private Text filterText() {
        return Text.translatable("screen.dotmod.preset.helper.filter." + filter.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double amount = verticalAmount == 0.0 ? horizontalAmount : verticalAmount;
        if (amount == 0.0) {
            return false;
        }
        boolean listVisible = width >= 520 || !compactDetails;
        if (listVisible && mouseX <= listWidth + 8) {
            int next = Math.max(0, Math.min(Math.max(0, filtered.size() - listRows), listFirst + (amount > 0 ? -1 : 1)));
            if (next != listFirst) {
                listFirst = next;
                refreshRows();
            }
            return true;
        }
        detailFirstLine = Math.max(0, detailFirstLine + (amount > 0 ? -1 : 1));
        return true;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, 0xE0101216);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 7, 0xFFFFFF);
        Text summary = Text.translatable(
                "screen.dotmod.preset.helper.summary",
                model.progress().available(), model.progress().required(),
                model.progress().missing(), model.progress().percentage()
        );
        context.drawCenteredTextWithShadow(textRenderer, summary, width / 2, 18,
                model.progress().complete() ? 0x55FF55 : 0xFFAA55);
        boolean detailsVisible = width >= 520 || compactDetails;
        if (detailsVisible) {
            renderDetails(context);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
        for (PresetRequirementRowWidget row : rows) {
            if (row.visible && row.isMouseOver(mouseX, mouseY) && row.requirement() != null) {
                context.drawItemTooltip(textRenderer, row.requirement().stack(), mouseX, mouseY);
                break;
            }
        }
    }

    private void renderDetails(DrawContext context) {
        int x = width < 520 ? 8 : listWidth + 16;
        int panelWidth = width - x - 8;
        int top = 56;
        context.fill(x, top, x + panelWidth, height - 32, 0xFF555555);
        context.fill(x + 1, top + 1, x + panelWidth - 1, height - 33, 0xD0181818);
        if (detailsDirty) {
            List<OrderedText> wrapped = new ArrayList<>();
            for (Text line : buildDetailLines()) {
                wrapped.addAll(textRenderer.wrapLines(line, panelWidth - 10));
            }
            detailCache = List.copyOf(wrapped);
            detailsDirty = false;
        }
        int visibleLines = Math.max(1, (height - top - 40) / 10);
        detailFirstLine = Math.min(detailFirstLine, Math.max(0, detailCache.size() - visibleLines));
        int y = top + 5;
        for (int index = detailFirstLine; index < Math.min(detailCache.size(), detailFirstLine + visibleLines); index++) {
            context.drawTextWithShadow(textRenderer, detailCache.get(index), x + 5, y, 0xDDDDDD);
            y += 10;
        }
    }

    private List<Text> buildDetailLines() {
        if (selected == null) {
            return List.of(Text.translatable("screen.dotmod.preset.helper.select"));
        }
        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("screen.dotmod.preset.helper.item", selected.stack().getName()));
        lines.add(Text.translatable(
                "screen.dotmod.preset.helper.counts",
                selected.required(), selected.available(), selected.missing(), selected.percentage()
        ));
        lines.add(Text.translatable(model.includesContainer()
                ? "screen.dotmod.preset.helper.source.container"
                : "screen.dotmod.preset.helper.source.player"));
        List<RecipeOption> recipes = model.recipes(selected);
        if (recipes.isEmpty()) {
            lines.add(Text.translatable("screen.dotmod.preset.helper.recipe.unavailable"));
            return lines;
        }
        RecipeOption recipe = recipes.get(recipeIndex);
        RecipeAvailability availability = model.availability(selected, recipe);
        lines.add(Text.translatable("screen.dotmod.preset.helper.recipe.output", recipe.outputCount()));
        if (!recipe.requirementsKnown()) {
            lines.add(Text.translatable("screen.dotmod.preset.helper.recipe.unknown"));
            return lines;
        }
        lines.add(Text.translatable(
                "screen.dotmod.preset.helper.recipe.crafts",
                availability.craftsNeeded(), availability.craftsPossibleNow()
        ));
        lines.add(Text.translatable("screen.dotmod.preset.helper.ingredients"));
        for (RecipeAvailability.IngredientStatus ingredient : availability.ingredients()) {
            Text name = ingredient.ingredient().alternatives().getFirst().exemplar().getName();
            lines.add(Text.translatable(
                    "screen.dotmod.preset.helper.ingredient",
                    name, ingredient.available(), ingredient.required(), ingredient.missing(),
                    ingredient.ingredient().alternatives().size()
            ));
        }
        lines.add(Text.translatable("screen.dotmod.preset.helper.tree"));
        appendTree(lines, model.dependencyTree(selected, recipe).root(), 0);
        return lines;
    }

    private static void appendTree(
            List<Text> lines,
            RecipeDependencyTree.ItemNode node,
            int depth
    ) {
        String indent = "  ".repeat(Math.min(depth, 6));
        lines.add(Text.literal(indent).append(Text.translatable(
                "screen.dotmod.preset.helper.tree.node",
                node.item().exemplar().getName(), node.required(),
                Text.translatable("screen.dotmod.preset.helper.tree.state." + node.state().name().toLowerCase(Locale.ROOT))
        )));
        for (RecipeDependencyTree.RecipeNode recipe : node.recipes()) {
            for (RecipeDependencyTree.ItemNode ingredient : recipe.ingredients()) {
                appendTree(lines, ingredient, depth + 1);
            }
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
