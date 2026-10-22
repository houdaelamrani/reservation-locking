@echo off
chcp 65001 > nul
echo ===============================================================================
echo   TP VERROUILLAGE OPTIMISTE JPA / HIBERNATE - HOUDA EL AMRANI
echo   EXECUTION DE LA SUITE DE TESTS JUNIT 4
echo ===============================================================================
call mvn test
pause
