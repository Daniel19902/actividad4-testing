package edu.unisabana.tyvs.registry.application.usecase;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;
import edu.unisabana.tyvs.registry.domain.model.Gender;
import edu.unisabana.tyvs.registry.domain.model.Person;
import edu.unisabana.tyvs.registry.domain.model.RegisterResult;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Clase de prueba unitaria para {@link Registry} utilizando un mock de {@link RegistryRepositoryPort}.
 *
 * <p>Estas pruebas ilustran cómo aislar el caso de uso del repositorio real,
 * aplicando dobles de prueba (Mockito) para simular los escenarios.</p>
 *
 * <p><b>Formato AAA:</b></p>
 * <ul>
 *   <li><b>Arrange</b>: se preparan datos y comportamiento del mock.</li>
 *   <li><b>Act</b>: se ejecuta el método bajo prueba.</li>
 *   <li><b>Assert</b>: se verifican resultados y que no haya interacciones no deseadas.</li>
 * </ul>
 *
 * <p><b>Beneficio:</b> este tipo de prueba es una <i>unitaria pura</i>,
 * sin necesidad de levantar bases de datos ni infraestructura adicional.</p>
 */
public class RegistryWithMockTest {

    /** Mock del puerto de persistencia. */
    private RegistryRepositoryPort repo;

    /** Caso de uso bajo prueba, instanciado con el mock. */
    private Registry registry;

    /**
     * Configura el mock y el caso de uso antes de cada prueba.
     *
     * <p>Se crea un mock de {@link RegistryRepositoryPort} usando Mockito
     * y se inyecta en la instancia de {@link Registry}.</p>
     */
    @Before
    public void setUp() {
        repo = mock(RegistryRepositoryPort.class);
        registry = new Registry(repo);
    }

    /**
     * Caso de prueba: detectar registros duplicados.
     *
     * <p><b>Escenario (BDD):</b></p>
     * <ul>
     *   <li><b>Given</b>: una persona con ID=7 y el repositorio ya indica que ese ID existe.</li>
     *   <li><b>When</b>: se intenta registrar la persona.</li>
     *   <li><b>Then</b>: el resultado debe ser {@link RegisterResult#DUPLICATED}
     *       y no se debe invocar el método {@code save(...)} en el repositorio.</li>
     * </ul>
     *
     * @throws Exception propagada en caso de error durante la ejecución.
     */
    @Test
    public void shouldReturnDuplicatedWhenRepoSaysExists() throws Exception {
        // Arrange: configurar mock y datos
        when(repo.existsById(7)).thenReturn(true);
        Person p = new Person("Ana", 7, 25, Gender.FEMALE, true);

        // Act: ejecutar método bajo prueba
        RegisterResult result = registry.registerVoter(p);

        // Assert: verificar resultado y comportamiento esperado del mock
        assertEquals(RegisterResult.DUPLICATED, result);
        verify(repo, never()).save(anyInt(), anyString(), anyInt(), anyBoolean());
    }

    /**
     * Caso de prueba: registrar exitosamente un votante válido.
     *
     * <p><b>Escenario:</b></p>
     * <ul>
     *   <li><b>Given</b>: un votante con ID=15 y el repositorio indica que no existe.</li>
     *   <li><b>When</b>: se registra al votante.</li>
     *   <li><b>Then</b>: el resultado debe ser {@link RegisterResult#VALID}
     *       y se debe llamar al método {@code save(...)} con los parámetros correspondientes.</li>
     * </ul>
     */
    @Test
    public void shouldSaveValidPerson() throws Exception {
        // Arrange
        when(repo.existsById(15)).thenReturn(false);
        Person p = new Person("Pedro", 15, 20, Gender.MALE, true);

        // Act
        RegisterResult result = registry.registerVoter(p);

        // Assert
        assertEquals(RegisterResult.VALID, result);
        verify(repo, times(1)).save(15, "Pedro", 20, true);
    }

    /**
     * Caso de prueba: manejar una excepción controlada del repositorio.
     *
     * <p><b>Escenario:</b></p>
     * <ul>
     *   <li><b>Given</b>: un votante válido pero el repositorio arroja un fallo de base de datos.</li>
     *   <li><b>When</b>: se intenta guardar al votante.</li>
     *   <li><b>Then</b>: se captura la excepción del repositorio y se lanza una {@link IllegalStateException} con detalles.</li>
     * </ul>
     */
    @Test
    public void shouldHandleRepositoryException() throws Exception {
        // Arrange
        Person p = new Person("Juan", 12, 30, Gender.MALE, true);
        when(repo.existsById(12)).thenReturn(false);
        doThrow(new RuntimeException("Conexión perdida con H2")).when(repo).save(anyInt(), anyString(), anyInt(), anyBoolean());

        // Act & Assert
        try {
            registry.registerVoter(p);
            fail("Se esperaba una IllegalStateException debido a fallo en repositorio");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Persistencia: RuntimeException - Conexión perdida con H2"));
        }
    }
}
