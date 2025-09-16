package com.stdnullptr.pokeball.command.suggestion;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.stdnullptr.pokeball.service.StasisService;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.List;

/**
 * Centralized suggestion providers for Pokeball commands
 */
public final class PokeballSuggestionProviders {

    private final StasisService stasisService;

    public PokeballSuggestionProviders(final StasisService stasisService) {
        this.stasisService = stasisService;
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
}