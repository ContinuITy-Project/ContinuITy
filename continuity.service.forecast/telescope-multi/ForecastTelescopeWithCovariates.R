forecast.period <- as.numeric(period)
foo <- telescope.forecast(tvp = intensities, horizon = forecast.period, hist.covar = hist.covar.matrix, future.covar = future.covar.matrix)
forecastValues <- as.numeric(foo$mean)