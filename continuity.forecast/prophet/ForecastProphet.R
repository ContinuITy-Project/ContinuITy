forecast <- predict(m, future)
forecastValues <- tail(forecast[['yhat']], forecast.period)
