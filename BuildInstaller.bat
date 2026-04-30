:: === Download FFmpeg if not present ===

if exist packaging\ffmpeg\ffmpeg.exe (
    echo FFmpeg already present, skipping download.
) else (
    echo Downloading FFmpeg...

    mkdir packaging\ffmpeg 2>nul

    :: Download (Gyan.dev build)
    curl -L -o ffmpeg.zip https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip

    :: Extract
    powershell -Command "Expand-Archive -Path ffmpeg.zip -DestinationPath ffmpeg_temp"

    :: Copy ffmpeg.exe
    for /d %%d in (ffmpeg_temp\ffmpeg-*-essentials_build) do (
        copy "%%d\bin\ffmpeg.exe" packaging\ffmpeg\ffmpeg.exe
    )

    rmdir /s /q ffmpeg_temp 2>nul
    del ffmpeg.zip

    echo FFmpeg downloaded.
)

mkdir target\ffmpeg 2>nul
copy packaging\ffmpeg\ffmpeg.exe target\ffmpeg\

jlink --module-path "D:\Program Files\Java\openjfx-21.0.10_windows-x64_bin-jmods\javafx-jmods-21.0.10;%JAVA_HOME%\jmods" --add-modules javafx.controls,javafx.fxml,javafx.media,java.naming --output runtime
jpackage --name PhotoOrganizer --input target --main-jar PhotoOrganizer.jar --main-class com.photoOrganizer.Main --type exe --icon packaging/app.ico --runtime-image runtime --win-console --resource-dir packaging --app-version 2.0