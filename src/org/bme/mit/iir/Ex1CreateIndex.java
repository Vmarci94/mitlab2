package org.bme.mit.iir;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Ex1CreateIndex {

    final TermRecognizer termRec;
    final Map<String, Map<File, Integer>> index;

    // Üres index létrehozása
    public Ex1CreateIndex() throws IOException {
        termRec = new TermRecognizer();
        index = new HashMap<>();
    }

    // index betöltése bináris file-ból
    @SuppressWarnings("unchecked")
    public Ex1CreateIndex(File indexFile) throws IOException, ClassNotFoundException {
        termRec = new TermRecognizer();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexFile))) {
            index = (Map<String, Map<File, Integer>>) ois.readObject();
        }
    }

    // index mentése bináris file-ba
    public void saveIndex(File indexFile) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(indexFile))) {
            oos.writeObject(index);
        }
    }

    // egy cikk hozzáadása az indexhez
    public void addFile(File file) throws IOException {
        String content = Util.readFileAsString(file.toString());
        Map<String, Integer> termFreqs = termRec.termFrequency(content);
        for (Map.Entry<String, Integer> termFreq : termFreqs.entrySet()) {
            String term = termFreq.getKey();
            Integer freq = termFreq.getValue();
            if (!index.containsKey(term)) {
                index.put(term, new HashMap<>());
            }
            index.get(term).put(file, freq);
        }
    }

    // könyvtár (korpusz) rekurzív hozzáadása az indexhez
    public void addFolder(File folder) throws IOException {
        if (!folder.exists() || !folder.isDirectory())
            throw new RuntimeException("A '" + folder + "' nem egy könyvtár!");
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                addFolder(f);
            } else {
                addFile(f);
            }
        }
    }

    // kulcsszó előfordulásainak és gyakoriságának lekérdezése
    public Map<File, Integer> getOccurences(String keyword) {
        if (index.containsKey(keyword))
            return Collections.unmodifiableMap(index.get(keyword));
        else
            return Collections.emptyMap();
    }

    //új függvény a metszet keresésre
    public HashSet<String> findIntersection( Collection<List<String>> listlist) {
        HashSet<String> result = new HashSet<>();
        boolean first = true;
        for (Collection<String> collection : listlist) {
            if (first) {
                result.addAll(collection);
                first = false;
            } else {
                result.retainAll(collection);
            }
        }
        return result;
    }

    //módosított main
    // index létrehozása
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Használat:");
            System.err.println("  java " + Ex1CreateIndex.class.getName() + " korpusz_könyvtár keresendő,szavak,vesszővel,elválasztva");
            System.err.println("A korpusz könyvtárban található cikkekre létrehoz egy bináris index file-t a kulcsszavak előfordulásaival.");
            System.exit(-1);
        }


        File indexFile = new File("index.idx");
        Ex1CreateIndex idx = null;
        if(!indexFile.exists()) {
            File corpusFolder = new File(args[0]);
            idx = new Ex1CreateIndex();
            System.out.println("Korpusz beolvasása...");
            idx.addFolder(corpusFolder);
            System.out.println("Index mentése...");
            idx.saveIndex(indexFile);
            System.out.println("Kész.");
        } else {
            System.out.println("index építése");
            idx = new Ex1CreateIndex(indexFile);
            System.out.println("index építése kész");
        }

        List<String> words = null;
        if(args.length == 1) {
            System.out.println("nincsennek kereső szavak, így csinálunk");
            words = Arrays.asList("Clarkdale", "Intel");
        } else {
            words = Arrays.asList(args[1].split(","));
        }
        System.out.println("keresendő szavak:\n" + words);

        final Map<String, List<String>> wordsAndFiles = new HashMap<>();
        for(String word : words) {
            List<String> fileNames = idx.getOccurences(word).keySet().stream().map(File::getName).collect(Collectors.toList());
            wordsAndFiles.put(word, fileNames);
        }

        wordsAndFiles.forEach( (word, fileNames) -> {
            System.out.println(word + ": " + fileNames);
        });

        System.out.println("kozos: " + idx.findIntersection( wordsAndFiles.values() ));

    }
}