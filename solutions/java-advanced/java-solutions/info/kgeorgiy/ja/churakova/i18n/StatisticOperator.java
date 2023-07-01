package info.kgeorgiy.ja.churakova.i18n;

import info.kgeorgiy.ja.churakova.bank.exceptions.WrongArgumentsException;
import info.kgeorgiy.ja.churakova.i18n.collectors.*;
import info.kgeorgiy.ja.churakova.i18n.collectors.strings.SentenceCollector;
import info.kgeorgiy.ja.churakova.i18n.collectors.strings.WordCollector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static info.kgeorgiy.ja.churakova.i18n.Utilits.getBundle;

public class StatisticOperator {

    private final Map<StatType, StatCollector<?>> collectors = Map.of(
            StatType.SENTENCE, new SentenceCollector(),
            StatType.WORD, new WordCollector(),
            StatType.NUMBER, new NumberCollector(),
            StatType.DATE, new DateCollector(),
            StatType.SUM, new SumCollector()
    );
    ConcurrentMap<String, ConcurrentMap<StatType, Statistic<?>>> collected;

    // TO_DO: parallel gathering !!!

    public StatisticOperator() {
        collected = new ConcurrentHashMap<>();
    }

    public void collectStatistics(Locale inputLocale, String inputFile, StatType... stats) {
        try {
            String input = Files.readString(Path.of(inputFile), StandardCharsets.UTF_8);
            List<StatType> types = Arrays.stream(Objects.requireNonNull(stats)).toList();
            ConcurrentMap<StatType, Statistic<?>> statMap = new ConcurrentHashMap<>();
            if (types.contains(StatType.FULL)) {
                collectors.forEach((type, collector) ->
                        statMap.put(type, collector.collectStatistics(inputLocale, input))
                );
            } else {
                collectors.entrySet().stream().filter(entry -> types.contains(entry.getKey()))
                        .forEach(entry ->
                                statMap.put(entry.getKey(), entry.getValue().collectStatistics(inputLocale, input)));
            }
            collected.put(inputFile, statMap);


        } catch (InvalidPathException ip) {
            System.err.printf("Path of given file %s is invalid: %s%n", inputFile, ip.getMessage());
        } catch (IOException e) {
            System.err.printf("IOException: %s%n", e.getMessage());
        }
    }

    public void writeReport(String inputFile, String outputFile, Locale repLocale) {
        checkFile(inputFile);
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(outputFile), StandardCharsets.UTF_8)) {
            writer.write(getSummary(inputFile, getBundle(repLocale)));
            for (Statistic<?> stat : collected.get(inputFile).values()) {
                writer.write(stat.getReport(repLocale));
            }
        } catch (InvalidPathException ip) {
            throw new WrongArgumentsException(String.format("Path of given file %s is invalid: %s%n", outputFile, ip.getMessage()));
        } catch (IOException e) {
            throw new WrongArgumentsException(String.format("IOException: %s%n", e.getMessage()));
        }
    }

    private String getSummary(String file, ResourceBundle bundle) {
        StringBuilder summary = new StringBuilder(String.format("%s: %s%n%s%n",
                bundle.getString("ANALYZED_FILE"), file, bundle.getString("SUMMARY_STATISTIC")));
        collected.get(file).forEach((statType, statistic) -> {
            summary.append(String.format("\t%s statistic: %d%n", bundle.getString(statType + "S"),
                    statistic.getEntries()));
        });

        return summary.toString();
    }


    private void checkFile(String inputFile) {
        if (collected.get(inputFile) == null) {
            throw new WrongArgumentsException(String.format("Statistic not collected for file %s%n", inputFile));
        }
    }
}
