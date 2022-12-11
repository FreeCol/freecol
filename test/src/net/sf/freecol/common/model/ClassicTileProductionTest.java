package net.sf.freecol.common.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.production.TileProductionCalculator;
import net.sf.freecol.common.model.production.WorkerAssignment;
import net.sf.freecol.util.test.FreeColTestCase;

public class ClassicTileProductionTest extends FreeColTestCase {
    
    private static final File EXPECTED_DIRECTORY = new File("test/expected-data");
    private static final File ACTUAL_DIRECTORY = new File("test/data");
    private static final File verifiedResultFile  = new File(EXPECTED_DIRECTORY, "verified-tile-production.csv");
    private static final File expectedResultFile  = new File(EXPECTED_DIRECTORY, "expected-tile-production.csv");
    private static final File actualResultFile  = new File(ACTUAL_DIRECTORY, "actual-tile-production.csv");

    public void testTileProduction() throws Exception {
        produceActualResultFile(actualResultFile);
        
        /*
         * The "verifiedResultFile" contains results that have been manually confirmed in the classic game.
         */
        compareResultFiles(verifiedResultFile, actualResultFile);
        
        /*
         * The "expectedResultFile" is used for regression testing. This file can be
         * updated with the "actualResultFile" if changes are made to the
         * "verifiedResultFile".
         */
        compareResultFiles(expectedResultFile, actualResultFile);
    }

    private void produceActualResultFile(File actualResultFile) throws FileNotFoundException {
        final Game game = getStandardGame();
        
        final List<TileImprovementType> tileImprovementColumns = spec().getTileImprovementTypeList();
        final String tileImprovementFormatString = tileImprovementColumns.stream().map(t -> "%s;").reduce(String::concat).orElseThrow();
        
        try (PrintWriter out = new PrintWriter(actualResultFile)) {
            writeHeaderTo(out, tileImprovementColumns);
            
            for (TileType tileType : spec().getTileTypeList()) {
                for (GoodsType goodsType : spec().getFarmedGoodsTypeList()) {
                    for (ResourceType resourceType : nullAnd(tileType.getResourceTypeValues())) {
                        final List<List<TileImprovementTypeWithMagnitude>> tileImprovementPermutations = determineTileImprovementPermutations(tileType);
                        final List<UnitType> unitTypes = getUnitTypesThatShouldBeTestedFor(goodsType);
                        for (UnitType unitType : unitTypes) {                            
                            for (List<TileImprovementTypeWithMagnitude> tileImprovementsWithMagnitude : tileImprovementPermutations) {                                
                                for (int colonyProductionBonus=-2; colonyProductionBonus<=2; colonyProductionBonus++) {
                                    final ProductionTestCombination ptc = new ProductionTestCombination(tileType,
                                            goodsType, resourceType, unitType, tileImprovementsWithMagnitude, colonyProductionBonus);
                                    
                                    final List<Object> output = executeProductionTestCombination(ptc, game, tileImprovementColumns);
                                    writeOutputTo(out, output, tileImprovementFormatString);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private List<Object> executeProductionTestCombination(ProductionTestCombination ptc, Game game, List<TileImprovementType> tileImprovementColumns) {
        final UnitType defaultUnitType = spec().getDefaultUnitType();
        final UnitType pettyCriminalUnitType = spec().getUnitType("model.unit.pettyCriminal");
        final UnitType indenturedServantUnitType = spec().getUnitType("model.unit.indenturedServant");
        
        final Tile tile = new Tile(game, ptc.tileType, 0, 1);
        if (ptc.resourceType != null) {
            tile.addResource(new Resource(game, tile, ptc.resourceType));
        }
        for (TileImprovementTypeWithMagnitude titwm : ptc.tileImprovementsWithMagnitude) {
            TileImprovement tileImprovement = new TileImprovement(game, tile, titwm.getTileImprovementType(), null);
            tileImprovement.setMagnitude(titwm.getMagnitude());
            tileImprovement.setTurnsToComplete(0);
            tile.add(tileImprovement);
        }
        
        final ProductionType productionType = ProductionType.getBestProductionType(ptc.goodsType, ptc.tileType.getAvailableProductionTypes(false));
        final TileProductionCalculator tpc = new TileProductionCalculator(null, ptc.colonyProductionBonus);
        final ProductionInfo pi;
        if (ptc.unitType != null) {
            pi = tpc.getBasicProductionInfo(tile, new Turn(1), new WorkerAssignment(ptc.unitType, productionType), false);
        } else {
            final ProductionType unattendedProductionType = ProductionType.getBestProductionType(ptc.goodsType, ptc.tileType.getAvailableProductionTypes(true));
            pi = tpc.getBasicProductionInfo(tile, new Turn(1), new WorkerAssignment(null, unattendedProductionType), true);
        }
        if (ptc.unitType == defaultUnitType) {                                        
            final ProductionInfo piCriminal = tpc.getBasicProductionInfo(tile, new Turn(1), new WorkerAssignment(pettyCriminalUnitType, productionType), false);
            assertEquals("Petty criminals should have the same production as a Free Colonist on tiles.",
                    getProductionAmount(ptc.goodsType, pi),
                    getProductionAmount(ptc.goodsType, piCriminal));
            
            final ProductionInfo piIndentured = tpc.getBasicProductionInfo(tile, new Turn(1), new WorkerAssignment(indenturedServantUnitType, productionType), false);
            assertEquals("Indentured Servants should have the same production as a Free Colonist on tiles.",
                    getProductionAmount(ptc.goodsType, pi),
                    getProductionAmount(ptc.goodsType, piIndentured));
        }
        
        final List<Object> output = new ArrayList<>();
        output.add(getProductionAmount(ptc.goodsType, pi));
        output.add(ptc.tileType.getId());
        output.add(ptc.goodsType.getId());
        output.add(ptc.resourceType != null ? ptc.resourceType.getId() : "");
        output.add(ptc.unitType != null ? ptc.unitType.getId() : "unattended");
        
        for (TileImprovementType columnType : tileImprovementColumns) {
            final int magnitude = ptc.tileImprovementsWithMagnitude.stream()
                .filter(t -> t.getTileImprovementType() == columnType)
                .findAny()
                .orElse(new TileImprovementTypeWithMagnitude(null, 0))
                .getMagnitude();
            output.add(magnitude);
        }
        
        output.add(Integer.toString(ptc.colonyProductionBonus));

        return output;
    }
    
    private void writeHeaderTo(PrintWriter out, List<TileImprovementType> tileImprovementColumns) {
        final String tileImprovementColumnHeaderString = tileImprovementColumns.stream().map(t -> t.getId() + ";").reduce(String::concat).orElseThrow();
        out.println("Production;Tile Type;Goods Type;Bonus Resource;Unit Type;" + tileImprovementColumnHeaderString + "Colony Production Bonus");
    }
    
    private void writeOutputTo(PrintWriter out, List<Object> output, String tileImprovementFormatString) {
        final String formatString = "%s;%s;%s;%s;%s;" + tileImprovementFormatString + "%s";
        final String result = String.format(formatString, output.toArray());
        out.println(result);
    }

    private List<UnitType> getUnitTypesThatShouldBeTestedFor( GoodsType goodsType) {
        final UnitType defaultUnitType = spec().getDefaultUnitType();
        final UnitType indianConvert = spec().getUnitType("model.unit.indianConvert");
        
        final List<UnitType> unitTypes = new ArrayList<>(List.of(defaultUnitType, indianConvert));
        unitTypes.add(null); // unattended
        
        final UnitType expertUnitType = spec().getExpertForProducing(goodsType);
        if (expertUnitType != null) {
            unitTypes.add(expertUnitType);
        }
        
        return unitTypes;
    }
    
    private void compareResultFiles(File expectedFile, File actualFile) throws IOException {
        final String header = readHeaderFromResultFile(expectedFile);
        final Map<String, Integer> expected = readResultFile(expectedFile);
        final Map<String, Integer> actual = readResultFile(actualFile);
        
        final StringBuilder sb = new StringBuilder();
        sb.append("There are actual production values that do not match the expected values.\n\n");
        sb.append("Expected Production;" + header + "\n");
        
        int failedChecks = 0;
        for (Entry<String, Integer> expectedEntry : expected.entrySet()) {
            final String key = expectedEntry.getKey();
            final Integer actualProduction = actual.get(key);
            if (actualProduction == null) {
                fail("Missing actual production for: " + key);
            }
            if (expectedEntry.getValue() != actualProduction) {
                failedChecks++;
                sb.append(expectedEntry.getValue() + ";" + actualProduction + ";" + key + "\n");
            }
        }
        assertEquals(sb.toString(), 0, failedChecks);
    }
    
    private String readHeaderFromResultFile(File resultFile) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(resultFile)))) {
            return in.readLine();
        }
    }
    
    private Map<String, Integer> readResultFile(File resultFile) throws IOException {
        final Map<String, Integer> result = new HashMap<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(resultFile)))) {
            String line;
            line = in.readLine(); // Ignore header.
            while ((line = in.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                final String[] a = line.split(";", 2);
                result.put(a[1], Integer.parseInt(a[0]));
            }
        }
        return result;
    }
    
    private int getProductionAmount(GoodsType goodstype, ProductionInfo pi) {
        if (pi.getProduction().isEmpty()) {
            return 0;
        }
        return pi.getProduction().stream()
                .filter(ag -> ag.getType().equals(goodstype))
                .map(AbstractGoods::getAmount)
                .findFirst()
                .orElse(0);
    }
    
    private <T> List<T> nullAnd(List<T> input) {
        final List<T> result = new ArrayList<>(input);
        result.add(null);
        return result;
    }
    
    private List<List<TileImprovementTypeWithMagnitude>> determineTileImprovementPermutations(TileType tileType) {
        final List<TileImprovementType> tileImprovements = spec().getTileImprovementTypeList()
                .stream()
                .filter(ti -> ti.isTileTypeAllowed(tileType))
                .collect(Collectors.toList());
        
        final int numberOfPermutations = (int) Math.pow(2, tileImprovements.size());
        final List<List<TileImprovementTypeWithMagnitude>> tileImprovementPermutations = new ArrayList<>(numberOfPermutations);
        for (int i=0; i<numberOfPermutations; i++) {
            final BitSet bitSet = BitSet.valueOf(new long[] { i });
            final List<TileImprovementTypeWithMagnitude> permutation = new ArrayList<>(tileImprovements.size());
            final List<TileImprovementTypeWithMagnitude> permutation2 = new ArrayList<>(tileImprovements.size());
            boolean hasRiver = false;
            for (int j=0; j<bitSet.length(); j++) {
                if (bitSet.get(j)) {
                    final TileImprovementType tileImprovementType = tileImprovements.get(j);
                    permutation.add(new TileImprovementTypeWithMagnitude(tileImprovementType, 1));
                    if (tileImprovementType.getId().equals("model.improvement.river")) {
                        permutation2.add(new TileImprovementTypeWithMagnitude(tileImprovementType, 2));
                        hasRiver = true;
                    } else {
                        permutation2.add(new TileImprovementTypeWithMagnitude(tileImprovementType, 1));
                    }
                }
            }
            
            tileImprovementPermutations.add(permutation);
            if (hasRiver) {
                tileImprovementPermutations.add(permutation2);
            }
        }
        return tileImprovementPermutations;
    }
    
    private static class ProductionTestCombination {
        private final TileType tileType;
        private final GoodsType goodsType;
        private final ResourceType resourceType;
        private final UnitType unitType;
        private final List<TileImprovementTypeWithMagnitude> tileImprovementsWithMagnitude;
        private final int colonyProductionBonus;
        
        public ProductionTestCombination(TileType tileType, GoodsType goodsType, ResourceType resourceType,
                UnitType unitType, List<TileImprovementTypeWithMagnitude> tileImprovementsWithMagnitude,
                int colonyProductionBonus) {
            this.tileType = tileType;
            this.goodsType = goodsType;
            this.resourceType = resourceType;
            this.unitType = unitType;
            this.tileImprovementsWithMagnitude = tileImprovementsWithMagnitude;
            this.colonyProductionBonus = colonyProductionBonus;
        }
    }
    
    private static class TileImprovementTypeWithMagnitude {
        private final TileImprovementType tileImprovementType;
        private final int magnitude;
        
        public TileImprovementTypeWithMagnitude(TileImprovementType tileImprovementType, int magnitude) {
            this.tileImprovementType = tileImprovementType;
            this.magnitude = magnitude;
        }
        
        public TileImprovementType getTileImprovementType() {
            return tileImprovementType;
        }
        
        public int getMagnitude() {
            return magnitude;
        }
    }
}
