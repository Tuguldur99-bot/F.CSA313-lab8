package mn.edu.must.sqat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;

    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    // A1: stub (when) + verify
    @Test
    void register_activeStudentWithPrerequisitesMet_returnsOkSavesAndSendsEmail() {
        Student student = new Student("S1", "s1@must.edu.mn", true, List.of("CS101"));
        when(repository.findStudent("S1")).thenReturn(student);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("CS101"));

        String result = service.register("S1", "CS201");

        assertEquals("OK", result);
        verify(repository).saveRegistration("S1", "CS201");
        verify(emailSender).send(eq("s1@must.edu.mn"), anyString(), anyString());
    }

    // A2: verify(..., never())
    @Test
    void register_unknownStudent_returnsNoStudentAndDoesNotSaveOrEmail() {
        when(repository.findStudent("GHOST")).thenReturn(null);

        String result = service.register("GHOST", "CS201");

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3: verifyNoInteractions
    @Test
    void register_inactiveStudent_returnsInactiveAndNeverTouchesEmailSender() {
        Student student = new Student("S2", "s2@must.edu.mn", false, List.of());
        when(repository.findStudent("S2")).thenReturn(student);

        String result = service.register("S2", "CS201");

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A4: stub + never (prerequisite partly missing)
    @Test
    void register_missingPrerequisite_returnsPrerequisitesErrorAndDoesNotSave() {
        Student student = new Student("S3", "s3@must.edu.mn", true, List.of("CS101"));
        when(repository.findStudent("S3")).thenReturn(student);
        when(repository.courseExists("CS301")).thenReturn(true);
        when(repository.prerequisites("CS301")).thenReturn(List.of("CS101", "CS102"));

        String result = service.register("S3", "CS301");

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A5: doThrow(...).when(...) - email failure must not break the registration
    @Test
    void register_emailSenderThrows_stillReturnsOkAndSaves() {
        Student student = new Student("S4", "s4@must.edu.mn", true, List.of());
        when(repository.findStudent("S4")).thenReturn(student);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP down"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register("S4", "CS101");

        assertEquals("OK", result);
        verify(repository).saveRegistration("S4", "CS101");
    }

    // A6: assertThrows - each argument being null is checked separately
    @Test
    void register_nullArgument_throwsIllegalArgumentAndTouchesNothing() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, "CS201"));
        assertThrows(IllegalArgumentException.class, () -> service.register("S1", null));

        verifyNoInteractions(repository, emailSender);
    }

    // A7: ArgumentCaptor - the email body contains the course code
    @Test
    void register_success_emailBodyContainsCourseCode() {
        Student student = new Student("S5", "s5@must.edu.mn", true, List.of());
        when(repository.findStudent("S5")).thenReturn(student);
        when(repository.courseExists("CS450")).thenReturn(true);
        when(repository.prerequisites("CS450")).thenReturn(List.of());
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        service.register("S5", "CS450");

        verify(emailSender).send(eq("s5@must.edu.mn"), anyString(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("CS450"));
    }

    // A8: three error classes in one parameterized test.
    // Each row needs a different stub setup, so the stubs are lenient():
    // with strict stubs, the stub a row never reaches would fail the test
    // with UnnecessaryStubbingException.
    @ParameterizedTest(name = "{0} -> {4}")
    @CsvSource({
            "noStudent, false, true,  true,  ERROR_NO_STUDENT",
            "inactive,  true,  false, true,  ERROR_INACTIVE_STUDENT",
            "noCourse,  true,  true,  false, ERROR_NO_COURSE"
    })
    void register_errorClass_returnsExpectedErrorAndDoesNotSaveOrEmail(
            String scenario, boolean studentExists, boolean active,
            boolean courseExists, String expected) {
        Student student = studentExists
                ? new Student("S6", "s6@must.edu.mn", active, List.of())
                : null;
        lenient().when(repository.findStudent("S6")).thenReturn(student);
        lenient().when(repository.courseExists("CS201")).thenReturn(courseExists);

        String result = service.register("S6", "CS201");

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }
}
