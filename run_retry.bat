@echo off
chcp 65001 > nul
echo ===============================================================================
echo   TP VERROUILLAGE OPTIMISTE JPA / HIBERNATE - HOUDA EL AMRANI
echo   SCENARIO 3 : RESOLUTION AUTOMATIQUE PAR RETRY HANDLER
echo ===============================================================================
call mvn compile exec:java "-Dexec.mainClass=com.coworking.concurrency.ConcurrentReservationWithRetry"
pause
