forecast.period <- as.numeric(period)
foo <- telescope.forecast(tvp = intensities, horizon = forecast.period)
forecastValues <- as.numeric(foo$mean)