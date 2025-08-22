package hudson.maven.reporters;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import hudson.maven.reporters.SurefireArchiver.FilteredReportsFileIterable;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;

class SurefireArchiverFilterReportsFileTest {

    @Test
    void shouldIncludeAllReportFiles() {
        File reportsDir = new File("bla");
        String[] reportFiles = { "a", "b", "c" };
        FilteredReportsFileIterable iterable = new FilteredReportsFileIterable(reportsDir, reportFiles, 0, 1000);
        
        iterable = spy(iterable);
        File file = mock(File.class);
        when(file.lastModified()).thenReturn(500L);
        
        when(iterable.getFile(any(File.class), anyString())).thenReturn(file);
        
        Iterator<File> iterator = iterable.iterator();
        
        iterator.next();
        iterator.next();
        iterator.next();
        
        assertThrows(NoSuchElementException.class, iterator::next, "Iterator should only have 3 elements");
    }

    @Test
    void shouldExcludeReportFileTooOld() {
        File reportsDir = new File("bla");
        String[] reportFiles = { "a", "b", "c" };
        FilteredReportsFileIterable iterable = new FilteredReportsFileIterable(reportsDir, reportFiles, 5000, 10000);
        
        iterable = spy(iterable);
        File included = mock(File.class);
        when(included.lastModified()).thenReturn(6000L);
        
        File tooOld = mock(File.class);
        when(tooOld.lastModified()).thenReturn(500L);
        
        when(iterable.getFile(any(File.class), anyString())).thenReturn(included, tooOld, included);
        
        Iterator<File> iterator = iterable.iterator();
        
        iterator.next();
        iterator.next();

        assertThrows(NoSuchElementException.class, iterator::next, "Iterator should only have 2 elements");
    }

    @Test
    void shouldExcludeReportFileTooYoung() {
        File reportsDir = new File("bla");
        String[] reportFiles = { "a", "b", "c" };
        FilteredReportsFileIterable iterable = new FilteredReportsFileIterable(reportsDir, reportFiles, 5000, 10000);
        
        iterable = spy(iterable);
        File included = mock(File.class);
        when(included.lastModified()).thenReturn(5000L);
        
        File tooYoung = mock(File.class);
        when(tooYoung.lastModified()).thenReturn(20000L);
        
        when(iterable.getFile(any(File.class), anyString())).thenReturn(included, included, tooYoung);
        
        Iterator<File> iterator = iterable.iterator();
        
        iterator.next();
        iterator.next();

        assertThrows(NoSuchElementException.class, iterator::next, "Iterator should only have 2 elements");
    }

}
