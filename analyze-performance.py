#!/usr/bin/env python3
"""
Performance Analysis Script for Locust Test Results
Analyzes response times, throughput, and error rates
"""

import csv
import json
import sys
from datetime import datetime
from pathlib import Path


class PerformanceAnalyzer:
    """Analyzes performance test results from Locust"""
    
    # Performance thresholds
    THRESHOLDS = {
        'max_avg_response_time': 2000,  # ms
        'max_95_percentile': 3000,      # ms
        'max_error_rate': 5.0,          # percentage
        'min_rps': 10                   # requests per second
    }
    
    def __init__(self, stats_file, history_file=None):
        self.stats_file = stats_file
        self.history_file = history_file
        self.results = {}
        self.violations = []
        
    def parse_stats(self):
        """Parse Locust statistics CSV file"""
        with open(self.stats_file, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                if row['Type'] == 'Aggregated':
                    self.results = {
                        'total_requests': int(row['Request Count']),
                        'failure_count': int(row['Failure Count']),
                        'avg_response_time': float(row['Average Response Time']),
                        'min_response_time': float(row['Min Response Time']),
                        'max_response_time': float(row['Max Response Time']),
                        'median_response_time': float(row['Median Response Time']),
                        'percentile_95': float(row['95%']) if '95%' in row else 0,
                        'percentile_99': float(row['99%']) if '99%' in row else 0,
                        'requests_per_sec': float(row['Requests/s']),
                        'failures_per_sec': float(row['Failures/s']),
                    }
                    
                    # Calculate error rate
                    if self.results['total_requests'] > 0:
                        self.results['error_rate'] = (
                            self.results['failure_count'] / 
                            self.results['total_requests'] * 100
                        )
                    else:
                        self.results['error_rate'] = 0
                    
                    break
    
    def check_thresholds(self):
        """Check if results meet performance thresholds"""
        violations = []
        
        if self.results['avg_response_time'] > self.THRESHOLDS['max_avg_response_time']:
            violations.append(
                f"❌ Average response time ({self.results['avg_response_time']:.2f}ms) "
                f"exceeds threshold ({self.THRESHOLDS['max_avg_response_time']}ms)"
            )
        
        if self.results['percentile_95'] > self.THRESHOLDS['max_95_percentile']:
            violations.append(
                f"❌ 95th percentile ({self.results['percentile_95']:.2f}ms) "
                f"exceeds threshold ({self.THRESHOLDS['max_95_percentile']}ms)"
            )
        
        if self.results['error_rate'] > self.THRESHOLDS['max_error_rate']:
            violations.append(
                f"❌ Error rate ({self.results['error_rate']:.2f}%) "
                f"exceeds threshold ({self.THRESHOLDS['max_error_rate']}%)"
            )
        
        if self.results['requests_per_sec'] < self.THRESHOLDS['min_rps']:
            violations.append(
                f"❌ Throughput ({self.results['requests_per_sec']:.2f} RPS) "
                f"below threshold ({self.THRESHOLDS['min_rps']} RPS)"
            )
        
        self.violations = violations
        return len(violations) == 0
    
    def generate_report(self):
        """Generate performance analysis report"""
        report = []
        report.append("=" * 80)
        report.append("PERFORMANCE TEST ANALYSIS REPORT")
        report.append("=" * 80)
        report.append(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report.append("")
        
        # Summary statistics
        report.append("📊 SUMMARY STATISTICS")
        report.append("-" * 80)
        report.append(f"Total Requests:          {self.results['total_requests']:,}")
        report.append(f"Failed Requests:         {self.results['failure_count']:,}")
        report.append(f"Error Rate:              {self.results['error_rate']:.2f}%")
        report.append(f"Throughput:              {self.results['requests_per_sec']:.2f} RPS")
        report.append("")
        
        # Response time metrics
        report.append("⏱️  RESPONSE TIME METRICS (ms)")
        report.append("-" * 80)
        report.append(f"Average:                 {self.results['avg_response_time']:.2f}")
        report.append(f"Minimum:                 {self.results['min_response_time']:.2f}")
        report.append(f"Maximum:                 {self.results['max_response_time']:.2f}")
        report.append(f"Median:                  {self.results['median_response_time']:.2f}")
        report.append(f"95th Percentile:         {self.results['percentile_95']:.2f}")
        report.append(f"99th Percentile:         {self.results['percentile_99']:.2f}")
        report.append("")
        
        # Threshold validation
        report.append("✅ THRESHOLD VALIDATION")
        report.append("-" * 80)
        
        if not self.violations:
            report.append("✅ All performance thresholds met!")
            report.append("")
            report.append("  ✓ Average response time within limits")
            report.append("  ✓ 95th percentile within limits")
            report.append("  ✓ Error rate within limits")
            report.append("  ✓ Throughput within limits")
        else:
            report.append("❌ Performance threshold violations detected:")
            report.append("")
            for violation in self.violations:
                report.append(f"  {violation}")
        
        report.append("")
        report.append("=" * 80)
        
        return "\n".join(report)
    
    def save_json_report(self, output_file='performance_report.json'):
        """Save results as JSON for CI/CD integration"""
        data = {
            'timestamp': datetime.now().isoformat(),
            'metrics': self.results,
            'thresholds': self.THRESHOLDS,
            'violations': self.violations,
            'passed': len(self.violations) == 0
        }
        
        with open(output_file, 'w') as f:
            json.dump(data, f, indent=2)
        
        print(f"JSON report saved to: {output_file}")
    
    def run_analysis(self):
        """Execute complete analysis"""
        print("🔍 Analyzing performance test results...")
        print()
        
        self.parse_stats()
        passed = self.check_thresholds()
        
        report = self.generate_report()
        print(report)
        
        # Save reports
        with open('performance_report.txt', 'w') as f:
            f.write(report)
        
        self.save_json_report()
        
        # Exit with appropriate code
        if passed:
            print("\n✅ Performance test PASSED")
            return 0
        else:
            print("\n❌ Performance test FAILED")
            return 1


def main():
    """Main entry point"""
    if len(sys.argv) < 2:
        print("Usage: python analyze_performance.py <locust_stats.csv>")
        sys.exit(1)
    
    stats_file = sys.argv[1]
    
    if not Path(stats_file).exists():
        print(f"Error: Stats file not found: {stats_file}")
        sys.exit(1)
    
    analyzer = PerformanceAnalyzer(stats_file)
    exit_code = analyzer.run_analysis()
    sys.exit(exit_code)


if __name__ == '__main__':
    main()