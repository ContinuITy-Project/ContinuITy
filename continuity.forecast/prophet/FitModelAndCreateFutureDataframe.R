m <- fit.prophet(m, df)		
forecast.period <- as.numeric(period)	
future <- make_future_dataframe(m, periods = forecast.period, freq = 60 * 60)
