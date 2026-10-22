@echo off
chcp 65001 > nul
echo ===============================================================================
echo   TP VERROUILLAGE OPTIMISTE JPA / HIBERNATE - HOUDA EL AMRANI
echo   SCENARIO 1 & 2 : SIMULATION DES CONFLITS DE CONCURRENCE
echo ===============================================================================
call mvn compile exec:java "-Dexec.mainClass=com.coworking.concurrency.ConcurrentReservationSimulator"
pause
