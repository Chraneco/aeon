package com.ultimatesoftware.aeon.core.command.execution.consumers;

import com.ultimatesoftware.aeon.core.framework.abstraction.drivers.IDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class CommandDelegateRunnerTests {

    private CommandDelegateRunner commandDelegateRunner;

    @Mock
    private Consumer<IDriver> consumer;

    @Mock
    private IDriver driver;

    @Mock
    private Function<IDriver, Object> commandDelegate;

    @BeforeEach
    void setUp() {
        this.commandDelegateRunner = new CommandDelegateRunner(driver);
    }

    @Test
    void voidExecute_passingNullValue_ShouldNotCallAcceptMethod() {

        //Arrange

        //Act
        Executable action = () -> commandDelegateRunner.execute((Consumer<IDriver>) null);

        //Assert
        assertDoesNotThrow(action);
    }

    @Test
    void voidExecute_passingNotNullValue_accept() {

        //Arrange

        //Act
        commandDelegateRunner.execute(consumer);

        //Assert
        verify(consumer, times(1)).accept(driver);
    }

    @Test
    void objectExecute_PassingNullValue_ReturnNull() {

        // Arrange

        //Act
        Object response = commandDelegateRunner.execute((Function<IDriver, Object>) null);

        //Assert
        assertNull(response);
    }

    @Test
    void objectExecute_PassingNullValue_applyDriver() {

        //Arrange

        //Act
        commandDelegateRunner.execute(commandDelegate);

        //Assert
        verify(commandDelegate, times(1)).apply(driver);
    }
}
