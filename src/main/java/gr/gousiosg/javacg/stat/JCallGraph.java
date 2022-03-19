/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.classfile.ClassParser;

import static java.util.stream.Collectors.toMap;

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives
 * into a single call graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
public class JCallGraph {

    public static void main(String[] args) {
        try {
            for (String arg : args) {
                File f = new File(arg);
                if (!f.exists()) {
                    System.err.println("Jar file " + arg + " does not exist");
                }

                try (JarFile jar = new JarFile(f)) {
                    Stream<JarEntry> entries = enumerationAsStream(jar.entries());

                    Stream<Visited> visitedEntries = entries
                            .map(e -> visitEntryAsClass(arg, e))
                            .filter(Optional::isPresent)
                            .map(Optional::get);
                    Stream<Visited> processedEntries = calculateReferencesByOthers(visitedEntries);
                    String methodCalls = processedEntries.
                            map(v -> visitedToString(v)).
                            reduce(new StringBuilder(),
                                    StringBuilder::append,
                                    StringBuilder::append).toString();

                    BufferedWriter log = new BufferedWriter(new OutputStreamWriter(System.out));
                    log.write(methodCalls);
                    log.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Error while processing jar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ClassVisitor getClassVisitor(ClassParser cp) {
        try {
            return new ClassVisitor(cp.parse());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<Visited> visitEntryAsClass(String arg, JarEntry e) {
        if (e.isDirectory() || !e.getName().endsWith(".class"))
            return Optional.empty();

        ClassParser cp = new ClassParser(arg, e.getName());
        return Optional.of(getClassVisitor(cp).start());
    }

    private static Stream<Visited> calculateReferencesByOthers(Stream<Visited> visited) {
        List<Visited> vis = new ArrayList<>();
        Map<String, Integer> referencedMap = visited.map(v -> {
            vis.add(v);
            return v;
        }).map(v -> v.getCalled())
                .flatMap((Map<String, Integer> m) -> m.entrySet().stream())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));
        return vis.stream().map(v -> {
            if (referencedMap.containsKey(v.getCaller())) {
                v.setReferencedByOthers(referencedMap.get(v.getCaller()));
            }
            return v;
        });
    }

    private static String visitedToString(Visited v) {
        return toString(v);
    }

    private static String toString(Visited v) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : v.getCalled().entrySet()) {
            //         Caller,          Referenced by others,            Called,           Referenced count
            sb.append(v.getCaller() + "," + v.getReferencedByOthers() + "," + e.getKey() + "," + e.getValue().toString() + "\n");
        }
        return sb.toString();
    }

    public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                            public T next() {
                                return e.nextElement();
                            }

                            public boolean hasNext() {
                                return e.hasMoreElements();
                            }
                        },
                        Spliterator.ORDERED), false);
    }
}
