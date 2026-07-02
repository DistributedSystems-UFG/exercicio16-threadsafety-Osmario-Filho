import java.util.concurrent.atomic.AtomicLong;

/**
 * Cliente de teste para SynchronizedRGB.
 *
 * Objetivo 1 (atomicidade de UM metodo): varias threads escritoras alternam o
 * objeto entre duas cores validas (C1 e C2) via set(); threads leitoras chamam
 * getRGB(). Como set() e getRGB() sao sincronizados, cada getRGB() deve retornar
 * SEMPRE o valor de C1 ou o de C2 -- nunca um valor "rasgado" (torn read) com
 * componentes misturados. Contamos qualquer leitura fora desses dois valores.
 *
 * Objetivo 2 (limitacao com acao COMPOSTA): uma leitora le getRGB() e, em outra
 * chamada separada, getName(). Cada metodo e atomico isoladamente, mas a SEQUENCIA
 * de duas chamadas NAO e atomica: uma escritora pode trocar a cor entre as duas
 * leituras, produzindo um par inconsistente (rgb de C1 com nome de C2). Isso mostra
 * que sincronizar metodos individuais nao garante seguranca de sequencias deles.
 */
public class TestSynchronizedRGB {

    static final int N_WRITERS = 4;
    static final int N_READERS = 4;
    static final int ITERATIONS = 200_000;

    public static void main(String[] args) throws InterruptedException {
        // Objeto compartilhado
        final SynchronizedRGB color = new SynchronizedRGB(10, 20, 30, "C1");

        final int rgb1 = (10 << 16) | (20 << 8) | 30;      // valor de C1
        final int rgb2 = (200 << 16) | (100 << 8) | 50;    // valor de C2

        final AtomicLong tornReads = new AtomicLong();        // leituras de UM metodo invalidas
        final AtomicLong inconsistentPairs = new AtomicLong(); // pares (rgb,nome) inconsistentes
        final AtomicLong pairChecks = new AtomicLong();

        Thread[] threads = new Thread[N_WRITERS + N_READERS];
        int idx = 0;

        // Escritoras: alternam entre C1 e C2
        for (int w = 0; w < N_WRITERS; w++) {
            threads[idx++] = new Thread(() -> {
                for (int i = 0; i < ITERATIONS; i++) {
                    if ((i & 1) == 0) color.set(10, 20, 30, "C1");
                    else              color.set(200, 100, 50, "C2");
                }
            });
        }

        // Leitoras
        for (int r = 0; r < N_READERS; r++) {
            threads[idx++] = new Thread(() -> {
                for (int i = 0; i < ITERATIONS; i++) {
                    // Teste 1: leitura de UM metodo sincronizado deve ser sempre valida
                    int rgb = color.getRGB();
                    if (rgb != rgb1 && rgb != rgb2) {
                        tornReads.incrementAndGet();
                    }

                    // Teste 2: acao composta (duas chamadas separadas)
                    int c = color.getRGB();
                    Thread.yield(); // aumenta a chance de a escritora agir entre as chamadas
                    String n = color.getName();
                    boolean ok = (c == rgb1 && n.equals("C1")) || (c == rgb2 && n.equals("C2"));
                    if (!ok) inconsistentPairs.incrementAndGet();
                    pairChecks.incrementAndGet();
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("=== SynchronizedRGB ===");
        System.out.println("Threads: " + N_WRITERS + " escritoras + " + N_READERS + " leitoras, "
                + ITERATIONS + " iteracoes cada.");
        System.out.println();
        System.out.println("[Teste 1] Leituras 'rasgadas' de getRGB() (valor invalido): "
                + tornReads.get()
                + (tornReads.get() == 0 ? "  -> OK: metodo individual e ATOMICO/threadsafe" : "  -> FALHA"));
        System.out.println();
        System.out.println("[Teste 2] Pares (rgb, nome) inconsistentes em acao composta: "
                + inconsistentPairs.get() + " de " + pairChecks.get() + " verificacoes");
        System.out.println("          -> Demonstra a LIMITACAO: cada metodo e atomico, mas a");
        System.out.println("             SEQUENCIA getRGB()+getName() nao e. O cliente precisa");
        System.out.println("             sincronizar externamente para ler um par consistente.");
    }
}
