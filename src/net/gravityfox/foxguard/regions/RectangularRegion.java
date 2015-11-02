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

package net.gravityfox.foxguard.regions;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.ArgumentParseException;
import net.gravityfox.foxguard.commands.util.InternalCommandState;
import net.gravityfox.foxguard.regions.util.BoundingBox2;
import net.gravityfox.foxguard.util.FGHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Fox on 8/18/2015.
 * Project: foxguard
 */
public class RectangularRegion extends OwnableRegionBase {

    private final BoundingBox2 boundingBox;


    public RectangularRegion(String name, BoundingBox2 boundingBox) {
        super(name);
        this.boundingBox = boundingBox;
    }

    public RectangularRegion(String name, List<Vector3i> positions, String[] args, CommandSource source)
            throws CommandException {
        super(name);
        List<Vector3i> allPositions = new LinkedList<>(positions);
        for (int i = 0; i < args.length - 1; i += 2) {
            int x, z;
            try {
                x = FGHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockX() : 0, args[i]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Texts.of("Unable to parse \"" + args[i] + "\"!"), e, args[i], i);
            }
            try {
                z = FGHelper.parseCoordinate(source instanceof Player ?
                        ((Player) source).getLocation().getBlockZ() : 0, args[i + 1]);
            } catch (NumberFormatException e) {
                throw new ArgumentParseException(
                        Texts.of("Unable to parse \"" + args[i + 1] + "\"!"), e, args[i + 1], i + 1);
            }
            allPositions.add(new Vector3i(x, 0, z));
        }
        if (allPositions.isEmpty()) throw new CommandException(Texts.of("No parameters specified!"));
        Vector3i a = allPositions.get(0), b = allPositions.get(0);
        for (Vector3i pos : allPositions) {
            a = a.min(pos);
            b = b.max(pos);
        }
        this.boundingBox = new BoundingBox2(a, b);
    }

    public RectangularRegion(String name, List<Vector3i> positions, String[] args, CommandSource source, User... owners) throws CommandException {
        this(name, positions, args, source);
        Collections.addAll(ownerList, owners);
    }

    public RectangularRegion(String name, List<Vector3i> positions, String[] args, CommandSource source, List<User> owners) throws CommandException {
        this(name, positions, args, source);
        this.ownerList = owners;
    }

    @Override
    public boolean modify(String arguments, InternalCommandState state, CommandSource source) {
        return false;
    }

    @Override
    public boolean isInRegion(int x, int y, int z) {
        return boundingBox.contains(x, z);
    }


    @Override
    public boolean isInRegion(double x, double y, double z) {
        return boundingBox.contains(x, z);
    }

    @Override
    public String getType() {
        return "Rect";
    }

    @Override
    public String getUniqueType() {
        return "rectangular";
    }


    @Override
    public Text getDetails(String arguments) {
        TextBuilder builder = Texts.builder();
        builder.append(super.getDetails(arguments));
        builder.append(Texts.of(TextColors.GREEN, "\nBounds: "));
        builder.append(Texts.of(TextColors.RESET, boundingBox.toString()));
        return builder.build();
    }

    @Override
    public void writeToDatabase(DataSource dataSource) throws SQLException {
        super.writeToDatabase(dataSource);
        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS BOUNDS(X INTEGER, Z INTEGER);" +
                    "DELETE FROM BOUNDS;" +
                    "INSERT INTO BOUNDS(X, Z) VALUES (" + boundingBox.a.getX() + ", " + boundingBox.a.getY() + ");" +
                    "INSERT INTO BOUNDS(X, Z) VALUES (" + boundingBox.b.getX() + ", " + boundingBox.b.getY() + ");");
        }
    }

    @Override
    public String toString() {
        return this.boundingBox.toString();
    }
}