package com.stdnullptr.pokeball.command.suggestion;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.stdnullptr.pokeball.config.ConfigManager;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.stdnullptr.pokeball.service.StasisService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centralized suggestion providers for Pokeball commands
 */
public final class PokeballSuggestionProviders {

    private static final Set<EntityType> EXCLUDED_ENTITY_TYPES = EnumSet.of(EntityType.UNKNOWN);

    private final StasisService stasisService;
    private final ConfigManager config;

    public PokeballSuggestionProviders(final StasisService stasisService, final ConfigManager config) {
        this.stasisService = stasisService;
        this.config = config;
    }

    /**
     * Suggests common item amounts (1, 2, 3, 5, 10, 16, 32, 64)
     */
    public SuggestionProvider<CommandSourceStack> amounts() {
        return (ctx, builder) -> {
            for (final String amount : List.of("1", "2", "3", "5", "10", "16", "32", "64")) {
                builder.suggest(amount);
            }
            return builder.buildFuture();
        };
    }

    /**
     * Suggests all current stasis IDs
     */
    public SuggestionProvider<CommandSourceStack> stasisIds() {
        return (ctx, builder) -> {
            for (final String id : stasisService.ids()) {
                builder.suggest(id);
            }
            return builder.buildFuture();
        };
    }

    /**
     * Suggests "all" plus all current stasis IDs
     */
    public SuggestionProvider<CommandSourceStack> stasisIdsWithAll() {
        return (ctx, builder) -> {
            builder.suggest("all");
            for (final String id : stasisService.ids()) {
                builder.suggest(id);
            }
            return builder.buildFuture();
        };
    }

    /**
     * Suggests common capacity limits (100, 250, 500, 1000)
     */
    public SuggestionProvider<CommandSourceStack> capacityLimits() {
        return (ctx, builder) -> {
            for (final String limit : List.of("100", "250", "500", "1000")) {
                builder.suggest(limit);
            }
            return builder.buildFuture();
        };
    }

    /**
     * Suggests refund modes (GIVE, DROP)
     */
    public SuggestionProvider<CommandSourceStack> refundModes() {
        return (ctx, builder) -> {
            builder.suggest("GIVE");
            builder.suggest("DROP");
            return builder.buildFuture();
        };
    }

    /**
     * Suggests entity types that can be added to the capture allow-list.
     */
    public SuggestionProvider<CommandSourceStack> capturableEntitiesToAdd() {
        return (ctx, builder) -> {
            final Set<String> alreadyAllowed = config
                    .capture()
                    .allowedTypes()
                    .stream()
                    .map(EntityType::name)
                    .collect(Collectors.toSet());

            final List<String> candidates = sortedEntityTypes()
                    .stream()
                    .map(EntityType::name)
                    .filter(name -> !alreadyAllowed.contains(name))
                    .toList();

            suggestMatching(candidates, builder);
            return builder.buildFuture();
        };
    }

    /**
     * Suggests entity types currently present in the capture allow-list.
     */
    public SuggestionProvider<CommandSourceStack> capturableEntitiesToRemove() {
        return (ctx, builder) -> {
            final List<String> allowed = config
                    .capture()
                    .allowedTypes()
                    .stream()
                    .map(EntityType::name)
                    .sorted()
                    .toList();

            suggestMatching(allowed, builder);
            return builder.buildFuture();
        };
    }

    private List<EntityType> sortedEntityTypes() {
        final List<EntityType> types = new ArrayList<>();
        for (final EntityType type : EntityType.values()) {
            if (EXCLUDED_ENTITY_TYPES.contains(type)) {
                continue;
            }
            types.add(type);
        }
        types.sort(Comparator.comparing(type -> type.name().toUpperCase(Locale.ENGLISH)));
        return types;
    }

    private void suggestMatching(final List<String> candidates, final SuggestionsBuilder builder) {
        final String remaining = builder.getRemainingLowerCase();

        for (final String candidate : candidates) {
            final String candidateLower = candidate.toLowerCase(Locale.ENGLISH);
            if (remaining.isEmpty() || candidateLower.startsWith(remaining)) {
                builder.suggest(candidate);
            }
        }
    }
}