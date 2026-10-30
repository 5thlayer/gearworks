// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import java.util.List;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;

/**
 * The Suit, worn in the chest slot (ADR 0002) and put on as any chestplate is. Its Gear Grid opens
 * from the worn suit with a key. Its FE, a Generator's spare fraction of one and the Combustion
 * Upgrade's fire change as it ticks.
 */
public final class SuitItem extends Item implements TickingGear {

    public SuitItem(Properties properties) {
        super(properties);
    }

    @Override
    public List<DataComponentType<?>> tickingComponents() {
        return List.of(GearContent.ENERGY.get(), GearContent.SPARE_FE.get(), GearContent.COMBUSTION_FIRE.get());
    }
}
