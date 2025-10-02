# Instrucciones para el Experimento de Comparación de Algoritmos MoraPack

Este documento proporciona instrucciones paso a paso para ejecutar el experimento de comparación entre los algoritmos ALNS y Búsqueda Tabú en el sistema MoraPack.

## Requisitos Previos

- Python 3.6 o superior
- Entorno virtual Python (venv)

## Configuración del Entorno

1. **Activar el entorno virtual**

   ```bash
   cd /Users/litus/Documents/Universidad/9-Semester/MoraPack/experiment
   source venv/bin/activate  # En Unix/macOS
   ```

2. **Instalar las dependencias**

   ```bash
   pip install -r requirements.txt
   ```

## Ejecución del Experimento

### Opción 1: Ejecutar el experimento completo

Este comando ejecutará 41 simulaciones para cada algoritmo, generará gráficos y producirá un informe detallado:

```bash
python run_simulations.py
```

### Opción 2: Generar solo datos de muestra

Si deseas generar datos de muestra sin ejecutar las simulaciones completas:

```bash
python generate_sample_data.py
```

### Opción 3: Analizar datos existentes

Si ya tienes datos generados y solo quieres analizarlos:

```bash
python analyze_results.py --csv results/simulation_results_YYYYMMDD-HHMMSS.csv
```

## Resultados

Después de ejecutar el experimento, encontrarás los siguientes archivos:

### Datos

- Archivo CSV con resultados: `results/simulation_results_YYYYMMDD-HHMMSS.csv`
- Informe generado automáticamente: `results/experiment_report_YYYYMMDD-HHMMSS.md`
- Informe final detallado: `results/FINAL_REPORT.md`

### Visualizaciones

- Gráficos QQ para evaluación de normalidad: `plots/qq_plot_alns.png` y `plots/qq_plot_tabusearch.png`
- Histogramas de distribución: `plots/histogram_alns.png` y `plots/histogram_tabusearch.png`
- Gráfico de comparación: `plots/algorithm_comparison.png`

## Interpretación de Resultados

El informe final (`FINAL_REPORT.md`) contiene un análisis detallado de los resultados, incluyendo:

1. Verificación de supuestos de normalidad
2. Pruebas estadísticas (Wilcoxon)
3. Comparación de rendimiento entre algoritmos
4. Conclusiones y recomendaciones

## Modificación del Experimento

Si deseas modificar parámetros del experimento, puedes editar las siguientes variables en `run_simulations.py`:

- `NUM_SIMULATIONS`: Número de simulaciones a ejecutar (predeterminado: 41)
- `SAVE_RESULTS`: Guardar resultados en CSV (predeterminado: True)
- `GENERATE_PLOTS`: Generar gráficos (predeterminado: True)
- `VERBOSE`: Mostrar información detallada durante la ejecución (predeterminado: True)

También puedes modificar la generación de datos en la función `generate_sample_data()` para ajustar las características de los datos simulados.
