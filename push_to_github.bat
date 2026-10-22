@echo off
chcp 65001 > nul
echo ===============================================================================
echo   PUSH DU PROJET VERS GITHUB - HOUDA EL AMRANI
echo   Dépôt distant : https://github.com/houdaelamrani/reservation-locking.git
echo ===============================================================================
echo.
echo Vérification et envoi de la branche 'main'...
git push -u origin main
echo.
if %ERRORLEVEL% EQU 0 (
    echo [SUCCÈS] Le projet a été publié avec succès sur GitHub !
) else (
    echo [ATTENTION] Le push a échoué.
    echo Assurez-vous d'avoir créé le dépôt vide sur GitHub :
    echo https://github.com/new avec le nom 'reservation-locking'
)
echo.
pause
