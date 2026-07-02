import java.util.concurrent.atomic.AtomicLong;

/**
 * Cliente de teste para ImmutableRGB.
 *
 * A ideia: um unico objeto imutavel e compartilhado por muitas threads SEM
 * qualquer sincronizacao. As threads leem getRGB()/getName() e chamam invert()
 * (que NAO altera o objeto: retorna uma NOVA instancia).
 *
 * Verificacoes:
 *  - O objeto compartilhado nunca muda: getRGB() e getName() devem devolver
 *    sempre os valores originais, em todas as threads (nenhuma violacao).
 *  - invert() produz corretamente o inverso e nao afeta o original.
 *  - invert() aplicado duas vezes retorna aos valores originais.
 */
public class TestImmutableRGB {

    static final int N_THREADS = 8;
    static final int ITERATIONS = 500_000;

    public static void main(String[] args) throws InterruptedException {
        // Objeto imutavel compartilhado -- criado uma vez, nunca modificado
        final ImmutableRGB base = new ImmutableRGB(10, 20, 30, "C1");

        final int expectedRGB = (10 << 16) | (20 << 8) | 30;
        final int expectedInvRGB = (245 << 16) | (235 << 8) | 225; // inverso de (10,20,30)
        final String expectedName = "C1";

        final AtomicLong violations = new AtomicLong();

        Thread[] threads = new Thread[N_THREADS];
        for (int t = 0; t < N_THREADS; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < ITERATIONS; i++) {
                    // Leituras sem qualquer lock -- seguras porque o estado e imutavel
                    if (base.getRGB() != expectedRGB)        violations.incrementAndGet();
                    if (!base.getName().equals(expectedName)) violations.incrementAndGet();

                    // invert() cria um novo objeto e nao toca no original
                    ImmutableRGB inv = base.invert();
                    if (inv.getRGB() != expectedInvRGB)      violations.incrementAndGet();

                    // Duplo invert deve reproduzir o original
                    ImmutableRGB back = inv.invert();
                    if (back.getRGB() != expectedRGB)        violations.incrementAndGet();

                    // O original permanece intacto apos os inverts
                    if (base.getRGB() != expectedRGB)        violations.incrementAndGet();
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("=== ImmutableRGB ===");
        System.out.println("Threads: " + N_THREADS + " (leem e invertem concorrentemente, SEM locks), "
                + ITERATIONS + " iteracoes cada.");
        System.out.println();
        System.out.println("Violacoes de consistencia detectadas: " + violations.get()
                + (violations.get() == 0
                    ? "  -> OK: objeto imutavel e THREADSAFE sem qualquer sincronizacao"
                    : "  -> FALHA"));
        System.out.println("Estado final do objeto compartilhado: rgb=" + base.getRGB()
                + ", name=" + base.getName() + " (inalterado)");
    }
}
