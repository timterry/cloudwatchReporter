package tterry.cloudwatchReporter;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by tim on 10/16/16.
 */
public class CloudwatchReporter extends ScheduledReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudwatchReporter.class);
    private static final double SMALLEST_SENDABLE = 1E-108;
    private static final double LARGEST_SENDABLE = 1E108;

    private AmazonCloudWatchClient cloudwatchClient = null;
    private String cloudwatchNamespace = null;
    private List<Dimension> dimensions = null;

    public CloudwatchReporter(MetricRegistry registry,
                              MetricFilter filter,
                              AmazonCloudWatchClient cloudwatchClient,
                              String cloudwatchNamespace,
                              Map<String,String> dimensions){
        super(registry, "CloudwatchReporter", filter, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
        this.cloudwatchClient = cloudwatchClient;
        this.cloudwatchNamespace = cloudwatchNamespace;
        this.dimensions = createDimensions(dimensions);
    }

    private List<Dimension> createDimensions(Map<String, String> ds){
        List<Dimension> dims = null;
        if(ds != null) {
            dims = new ArrayList<>(ds.size());
            for (Map.Entry<String, String> dim : ds.entrySet()) {
                Dimension d = new Dimension().withName(dim.getKey()).withValue(dim.getValue());
                dims.add(d);
            }
        }
        else{
            dims = Collections.EMPTY_LIST;
        }
        return dims;
    }

    private boolean isValueValid(double value){
        return (SMALLEST_SENDABLE < value) && (value < LARGEST_SENDABLE);
    }

    private MetricDatum reportValue(Date timestamp, String name, double value, StandardUnit unit){
        MetricDatum md = null;
        if(isValueValid(value)){
            md = new MetricDatum();
            md.setTimestamp(timestamp);
            md.setValue(value);
            md.setDimensions(this.dimensions);
            md.setMetricName(name);
            md.setUnit(unit);
        }
        return md;
    }

    private void sendMetrics(Collection<MetricDatum> metrics) {
        PutMetricDataRequest mr = new PutMetricDataRequest();
        mr.setMetricData(metrics);
        mr.setNamespace(this.cloudwatchNamespace);
        try {
            this.cloudwatchClient.putMetricData(mr);
        }
        catch(Exception e){
            LOGGER.warn("Unable to send metrics to aws cloudwatch", e);
        }
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {

    }
}
