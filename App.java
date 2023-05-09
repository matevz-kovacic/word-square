import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class App
{
    static final String ALPHABET      = "abcdefghijklmnopqrstuvwxyz";
    static final int    ALPHABET_SIZE = ALPHABET.length();

    public static void main(String[] args) {
        final int n = 10; // word square size

        System.out.println("start: " + now());

        final int[][] t = loadDictionary("./english-words-10-characters.txt", n); // trie of dictionary words of length n

        // trie set is used to determine a set of all possible next characters at trie node t[i][j] (representing string prefix)
        // and starting at d character from t[i][j]
        // ts[i][d][j] : trie set for trie node t[i][j] with at distance d from t[i][j] (i.e. d characters next to t[i][j])
        final boolean[][][] ts = trieSet(t, n);

        final int[] si = traversal(n, true);  // i coordinate for every step of the words square traversal
        final int[] sj = traversal(n, false);  // j coordinate for every step of the words square traversal
        final int nSteps = si.length;

        final int [][] m = new int[n][n];  // index of character in ALPHABET array placed in word square cell
        final int [][] h = new int[n][n];  // trie node index in horizontal direction of word square character
        final int [][] v = new int[n][n];  // trie node index in vertical direction of word square character

        // candidate characters: c[i][j][k] indicates that character with code k is a valid candidate at m[i][j]
        final boolean [][][] c = new boolean[n][n][ALPHABET_SIZE];

        final Character startCharacter = 'a';
        int step = 0;  // step index in word square creation
        boolean forward = false;
        m[0][0] = ALPHABET.indexOf(startCharacter) - 1;

        long iteration = 0;
        int maxStep = 0;
        int nSolutions = 0;

        for(;;) {
            final int i = si[step];
            final int j = sj[step];

            // hL = horizontal direction expansion of word prefix starting at m[i][0] and ending at m[i][j]
            final int[] hL = j == 0 ? t[0] : t[h[i][j - 1]];
            // vL = vertical direction expansion of word prefix starting at m[0][j] and ending at m[i][j]
            final int[] vL = i == 0 ? t[0] : t[v[i - 1][j]];

            // expansion of prefixes ending at transposed element m[j][i]
            // hU = horizontal direction expansion of word prefix starting at m[j][0] and ending at m[j][i]
            final int[] hU = i == 0 ? t[0] : t[h[j][i - 1]];
            // vU = vertical direction expansion of word prefix starting at m[0][i] and ending at m[j][i]
            final int[] vU = j == 0 ? t[0] : t[v[j - 1][i]];

            // try next character at position m[i][j]
            int match = forward ? 0 : m[i][j] + 1;

            // find a match character at m[i,j] in horizontal and vertical direction
            for (; match < ALPHABET_SIZE; ++match) {
                // check if extensions of word in range m[i][0].. m[i][j] in horizontal direction (hL[i][j])
                //                matches crossing word in range m[0][j].. m[i][j] in vertical direction (hL[i][j])
                // analogous test is performed for transposed element m[j][i]
                if (hL[match] == 0 || vL[match] == 0 || hU[match] == 0 || vU[match] == 0)
                    continue;

                // check if m[i][j] = match can be extended vertically towards m[i + j][j] (check only rows in the diagonal ending at m[i + j][0]
                // as lower rows are minimally constrained and won't contribute to pruning significantly
                if (j > 0) {
                    final boolean[][] vNode = ts[vL[match]];
                    boolean isMatch = true;
                    final int nRows = i + j >= n ? n - i : j;

                    for (int d = nRows; d > 0; --d) {
                        if (i + d >= n)
                            break;

                        final boolean[] s1 = vNode[d];
                        final int hNode = h[i + d][j - d];
                        final boolean[] s2 = ts[hNode][d];

                        boolean isSetMatch = false;
                        for (int k = 0; k < ALPHABET_SIZE; ++k) {
                            if (s1[k] && s2[k]) {
                                isSetMatch = true;
                                break;
                            }
                        }

                        if (!isSetMatch) {
                            isMatch = false;
                            break;
                        }
                    }

                    if (!isMatch)
                        continue;
                }

                // omitted due to no gain in program execution speed
                /* check rows below m[i][j] matching m[j][i] in vertical direction
                if ((i + 1) < n) {
                    final boolean[][] vNode = ts[vU[match]];
                    boolean isMatch = true;
                    final int nRows = i + j >= n ? n - i : j + 1;

                    for (int d = nRows - 1; d > 0; --d) {
                        final boolean[] s1 = vNode[d + i - j];
                        final int hNode = h[i + d][j - d];
                        final boolean[] s2 = ts[hNode][d + i - j];

                        boolean isSetMatch = false;
                        for (int k = 0; k < ALPHABET_SIZE; ++k) {
                            if (s1[k] && s2[k]) {
                                isSetMatch = true;
                                break;
                            }
                        }

                        if (!isSetMatch) {
                            isMatch = false;
                            break;
                        }
                    }

                    if (!isMatch)
                        continue;
                }
                */

                // check if m[j][i] = match can be extended vertically towards m[i + j][i] (check only rows in the diagonal ending at m[i + j - 1][0]
                // as lower rows are minimally constrained and won't contribute to pruning significantly
                if (i > j) {
                    final boolean[][] vNode = ts[vU[match]];
                    boolean isMatch = true;
                    final int nr = i - j;
                    for (int d = 1; d < nr; ++d) {

                        final boolean[] s1 = vNode[d];
                        final int hNode = h[j + d][i - d - 1];
                        final boolean[] s2 = ts[hNode][d + 1];

                        boolean isSetMatch = false;
                        for (int k = 0; k < ALPHABET_SIZE; ++k) {
                            if (s1[k] && s2[k]) {
                                isSetMatch = true;
                                break;
                            }
                        }

                        if (!isSetMatch) {
                            isMatch = false;
                            break;
                        }
                    }

                    if (!isMatch)
                        continue;
                }

                break;
            }


            if (match < ALPHABET_SIZE) {
                m[i][j] = match;
                m[j][i] = match;

                h[i][j] = hL[match];
                v[i][j] = vL[match];

                h[j][i] = hU[match];
                v[j][i] = vU[match];

                if (++step == nSteps) {
                    ++nSolutions;
                    System.out.printf("\nSolution %d found: loop count %,d  %s  \n\n", nSolutions, iteration, now());
                    display(m, i, j, si, sj, step);
                    --step;
                    forward = false;
                    continue;
                }

                if (step > maxStep && nSolutions == 0) {
                    System.out.printf("\n\n\nTemp solution after %,d iterations  STEP = %d/%d  %s\n\n",
                                        iteration, step, nSteps, now());
                    display(m, i, j, si, sj, step);
                    maxStep = step;
                }

                forward = true;
            } else {
                if (--step < 0)
                    break;

                if (step == 0)
                    System.out.printf("passed the first character %s    Number of iterations %,d    %s\n",
                                       ALPHABET.charAt(m[0][0]), iteration, now());
                forward = false;
            }

            ++iteration;
        }

        System.out.printf("\n\nNumber of solutions found: %d\ntotal loop count %,d\nfinish time  %s  \n\n",
                     nSolutions, iteration, now());
    }

    private static void display(int[][] m, int x, int y, int[] si, int[] sj, final int nStep) {
        final int n = m.length;

        boolean[][] o = new boolean[n][n];
        for (int step = 0; step < nStep; ++step) {
            o[si[step]][sj[step]] = true;
            o[sj[step]][si[step]] = true;
        }

        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                System.out.print(o[i][j] ? ALPHABET.charAt(m[i][j]) : '.');
            }
            System.out.println();
        }
        System.out.println("\n\n");
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static int[][] loadDictionary(final String fileName, final int n) {
        /*
            the boxed trie array is initially a 1x26 array, representing the root node of the trie with ALPHABET_SIZE
            possible children (one for each letter of the alphabet). When a new word is inserted into the trie,
            the implementation iterates over each character in the word, and for each character, checks if there is
            a child node for that character. If not, it creates a new row in the trie array to represent the child
            node, and updates the parent node's corresponding entry in the trie array to point to the new child node.
         */

        final List<String> words = loadDictionaryWords(fileName, n);
        if (words.size() < n)
            throw new RuntimeException("at least " + n + " words words of length " + n + " needed in dictionary " + fileName);

        final List<Integer> zeros = Collections.nCopies(ALPHABET_SIZE, 0);
        final ArrayList<ArrayList<Integer>> trie = new ArrayList<>();
        trie.add(new ArrayList<>(zeros)); // root of the trie

        for (final String word : words) {
            int node = 0;

            for (final char c : word.toCharArray()) {
                // map character to index which is in the [0..ALPHABET_SIZE) range
                final int index = ALPHABET.indexOf(c);
                if (trie.get(node).get(index) == 0) {
                    trie.get(node).set(index, trie.size()); // update pointer to new word
                    trie.add(new ArrayList<>(zeros)); // initialize new word
                }

                node = trie.get(node).get(index);
            }
        }

        // convert to 2D array
        int[][] result = new int[trie.size()][ALPHABET_SIZE];

        for (int i = 0, size = trie.size(); i < size; ++i) {
            for (int j = 0; j < ALPHABET_SIZE; ++j) {
                result[i][j] = trie.get(i).get(j);
            }
        }

        System.out.printf("%,d words of length %d loaded\n", words.size(), n);

        return result;
    }

    private static List<String> loadDictionaryWords(final String fileName, final int n) {
        final Set<String> set = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String word;
            while ((word = br.readLine()) != null) {
                if (word.length() != n ||
                        Character.isDigit(word.charAt(0)) ||
                        Character.isUpperCase(word.charAt(0)) ||
                        ! isValidWord(word))
                    continue;

                set.add(word);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        final List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    private static boolean[][][] trieSet(final int[][] trie, final int n) {
        final boolean[][][] result = new boolean[trie.length][n + 1][ALPHABET_SIZE];

        for (int i = 0, size = trie.length; i < size; ++i)
            for (int d = 0; d < n; ++d)
                for (final int charCode : trieSet(trie, i, d)) {
                    result [i][d + 1][charCode] = true;
                }

        return result;
    }

    private static Set<Integer> trieSet(final int[][] trie, final int node, final int distance) {
        final Set<Integer> result = new HashSet<>();
        final int[] subNodes = trie[node];

        for (int i = 0, size = subNodes.length; i < size; ++i) {
            final int nextNode = subNodes[i];
            if (nextNode == 0)
                continue;

            if (distance == 0)
                result.add(i);
            else
                result.addAll(trieSet(trie, nextNode, distance - 1));
        }

        return result;
    }

    private static int[] traversal(final int n, final boolean isIcoordinate) {
        final int[] result = new int[n * (n + 1) / 2];
        int idx = 0;

        for (int k = 0 ; k < n * 2 ; ++k) {
            for (int j = 0 ; j <= k ; ++j) {
                final int i = k - j;
                if (i < n && j <= i ) {
                    result[idx++] = isIcoordinate ? i : j;
                }
            }
        }

        return result;
    }

    private static boolean isValidWord(final String word) {
        for (final char c : word.toCharArray()) {
            if (ALPHABET.indexOf(c) == -1)
                return false;
        }
        return ! word.isEmpty();
    }
}
