#include <windows.h>
#include <stdio.h>
#include <tchar.h>
int _tmain(int argc, TCHAR *argv[]){
    
    STARTUPINFO si;
    PROCESS_INFORMATION pi;

    ZeroMemory( &si, sizeof(si) );
    si.cb = sizeof(si);
    ZeroMemory( &pi, sizeof(pi) );


    LPTSTR cmd = _tcsdup(TEXT("java -jar "
    //    "--module-path javafx-sdk-21\\lib "
    //    "--add-modules=javafx.controls,javafx.fxml "
        "server-1.0-SNAPSHOT-jar-with-dependencies.jar"));

    // Start the child process. 
    if( !CreateProcess(
        NULL,       // No module name (use command line)
        cmd,        // Command line
        NULL,       // Process handle not inheritable
        NULL,       // Thread handle not inheritable
        FALSE,      // Set handle inheritance to FALSE
        NULL, //CREATE_NO_WINDOW, // Creation flags
        NULL,       // Use parent's environment block
        NULL,       // Use parent's starting directory 
        &si,        // Pointer to STARTUPINFO structure
        &pi )       // Pointer to PROCESS_INFORMATION structure
        )
        {
        // CreateProcess failed.
        return 1;
    }

    // Wait until child process exits.
    WaitForSingleObject( pi.hProcess, INFINITE );

    // Close process and thread handles. 
    CloseHandle( pi.hProcess );
    CloseHandle( pi.hThread );

    return 0;
}