/*
 * This file is part of FoxGuard, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015. gravityfox - https://gravityfox.net/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.gravityfox.foxguard.commands;

import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.dispatcher.Disambiguator;
import org.spongepowered.api.util.command.dispatcher.SimpleDispatcher;
import net.gravityfox.foxguard.FoxGuardMain;
import net.gravityfox.foxguard.util.CallbackHashMap;
import net.gravityfox.foxguard.commands.util.InternalCommandState;

import java.util.Map;

/**
 * Created by Fox on 10/25/2015.
 * Project: foxguard
 */
public final class FGCommandMainDispatcher extends FGCommandDispatcher {

    private static FGCommandMainDispatcher instance;
    private final Map<CommandSource, InternalCommandState> stateMap = new CallbackHashMap<>((o, m) -> {
        if (o instanceof CommandSource) {
            InternalCommandState state = new InternalCommandState();
            m.put((CommandSource) o, state);
            return state;
        }
        return null;
    });

    public FGCommandMainDispatcher(String primaryAlias) {
        this(primaryAlias, SimpleDispatcher.FIRST_DISAMBIGUATOR);
    }

    public FGCommandMainDispatcher(String primaryAlias, Disambiguator disambiguatorFunc) {
        super(primaryAlias, disambiguatorFunc);
        instance = this;
    }

    public static FGCommandMainDispatcher getInstance() {
        return instance;
    }

    public Map<CommandSource, InternalCommandState> getStateMap() {
        return stateMap;
    }

    public PaginationService getPageService() {
        return FoxGuardMain.getInstance().getGame().getServiceManager().provide(PaginationService.class).get();
    }
}