#!/usr/bin/env python3
"""
Performance Test Runner for E-commerce Microservices
Automated test execution with comprehensive reporting
"""

import subprocess
import json
import csv
import time
import os
from datetime import datetime
from pathlib import Path

class PerformanceTestRunner:
    """Comprehensive performance test runner"""
    
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
        self.results_dir = Path("performance_results")
        self.results_dir.mkdir(exist_ok=True)
        
    def run_test_scenario(self, scenario_name, users, spawn_rate, run_time, 
                         locust_file="EnhancedLocustPerformanceTest.py"):
        """Run a specific test scenario"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_prefix = f"{scenario_name}_{timestamp}"
        
        # Locust command
        cmd = [
            "locust",
            "-f", locust_file,
            "--host", self.base_url,
            "--users", str(users),
            "--spawn-rate", str(spawn_rate),
            "--run-time", run_time,
            "--headless",
            "--html", f"{self.results_dir}/{output_prefix}_report.html",
            "--csv", f"{self.results_dir}/{output_prefix}_stats"
        ]
        
        print(f"Running {scenario_name} test...")
        print(f"Command: {' '.join(cmd)}")
        
        try:
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=3600)
            
            if result.returncode == 0:
                print(f"✅ {scenario_name} test completed successfully")
                return True
            else:
                print(f"❌ {scenario_name} test failed")
                print(f"Error: {result.stderr}")
                return False
                
        except subprocess.TimeoutExpired:
            print(f"⏰ {scenario_name} test timed out")
            return False
        except Exception as e:
            print(f"❌ Error running {scenario_name} test: {e}")
            return False
    
    def run_all_scenarios(self):
        """Run all performance test scenarios"""
        scenarios = {
            "normal_load": {"users": 50, "spawn_rate": 5, "run_time": "5m"},
            "stress_load": {"users": 200, "spawn_rate": 10, "run_time": "10m"},
            "spike_load": {"users": 500, "spawn_rate": 50, "run_time": "2m"},
            "endurance_load": {"users": 100, "spawn_rate": 2, "run_time": "30m"}
        }
        
        results = {}
        
        for scenario_name, config in scenarios.items():
            print(f"\n{'='*60}")
            print(f"Running {scenario_name.upper()} test")
            print(f"{'='*60}")
            
            success = self.run_test_scenario(
                scenario_name,
                config["users"],
                config["spawn_rate"],
                config["run_time"]
            )
            
            results[scenario_name] = success
            
            if success:
                self.analyze_results(scenario_name)
            
            # Wait between tests
            if scenario_name != "endurance_load":  # Don't wait after the last test
                print("Waiting 30 seconds before next test...")
                time.sleep(30)
        
        self.generate_summary_report(results)
        return results
    
    def analyze_results(self, scenario_name):
        """Analyze test results for a specific scenario"""
        print(f"\nAnalyzing results for {scenario_name}...")
        
        # Find the most recent stats file for this scenario
        stats_files = list(self.results_dir.glob(f"{scenario_name}_*_stats_stats.csv"))
        if not stats_files:
            print(f"No stats file found for {scenario_name}")
            return
        
        latest_stats = max(stats_files, key=os.path.getctime)
        
        try:
            with open(latest_stats, 'r') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    if row['Type'] == 'Aggregated':
                        self.print_metrics(row, scenario_name)
                        break
        except Exception as e:
            print(f"Error analyzing results: {e}")
    
    def print_metrics(self, row, scenario_name):
        """Print performance metrics"""
        print(f"\n📊 Performance Metrics for {scenario_name.upper()}")
        print("-" * 50)
        print(f"Total Requests:          {row['Request Count']:,}")
        print(f"Failed Requests:         {row['Failure Count']:,}")
        print(f"Average Response Time:   {row['Average Response Time']}ms")
        print(f"95th Percentile:         {row['95%']}ms")
        print(f"99th Percentile:         {row['99%']}ms")
        print(f"Requests per Second:     {row['Requests/s']}")
        print(f"Failure Rate:            {row['Failure Rate']}%")
        
        # Validate against thresholds
        self.validate_thresholds(row, scenario_name)
    
    def validate_thresholds(self, row, scenario_name):
        """Validate performance against thresholds"""
        thresholds = {
            "max_avg_response_time": 2000,  # ms
            "max_95_percentile": 3000,      # ms
            "max_error_rate": 5.0,          # percentage
            "min_rps": 10                   # requests per second
        }
        
        violations = []
        
        avg_response = float(row['Average Response Time'])
        percentile_95 = float(row['95%'])
        error_rate = float(row['Failure Rate'])
        rps = float(row['Requests/s'])
        
        if avg_response > thresholds['max_avg_response_time']:
            violations.append(f"Average response time ({avg_response}ms) exceeds threshold ({thresholds['max_avg_response_time']}ms)")
        
        if percentile_95 > thresholds['max_95_percentile']:
            violations.append(f"95th percentile ({percentile_95}ms) exceeds threshold ({thresholds['max_95_percentile']}ms)")
        
        if error_rate > thresholds['max_error_rate']:
            violations.append(f"Error rate ({error_rate}%) exceeds threshold ({thresholds['max_error_rate']}%)")
        
        if rps < thresholds['min_rps']:
            violations.append(f"RPS ({rps}) below threshold ({thresholds['min_rps']})")
        
        if violations:
            print(f"\n❌ Performance threshold violations detected:")
            for violation in violations:
                print(f"  - {violation}")
        else:
            print(f"\n✅ All performance thresholds met for {scenario_name}")
    
    def generate_summary_report(self, results):
        """Generate comprehensive summary report"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        report = {
            "timestamp": timestamp,
            "base_url": self.base_url,
            "test_results": results,
            "summary": {
                "total_tests": len(results),
                "passed_tests": sum(1 for success in results.values() if success),
                "failed_tests": sum(1 for success in results.values() if not success)
            }
        }
        
        # Save JSON report
        report_file = self.results_dir / f"performance_summary_{timestamp.replace(':', '-')}.json"
        with open(report_file, 'w') as f:
            json.dump(report, f, indent=2)
        
        # Print summary
        print(f"\n{'='*60}")
        print("PERFORMANCE TEST SUMMARY")
        print(f"{'='*60}")
        print(f"Timestamp: {timestamp}")
        print(f"Base URL: {self.base_url}")
        print(f"Total Tests: {report['summary']['total_tests']}")
        print(f"Passed: {report['summary']['passed_tests']}")
        print(f"Failed: {report['summary']['failed_tests']}")
        print(f"\nDetailed report saved to: {report_file}")
        
        # List all generated files
        print(f"\nGenerated files in {self.results_dir}:")
        for file in sorted(self.results_dir.glob("*")):
            print(f"  - {file.name}")
    
    def run_quick_test(self):
        """Run a quick smoke test"""
        print("Running quick smoke test...")
        return self.run_test_scenario(
            "smoke_test",
            users=10,
            spawn_rate=2,
            run_time="1m"
        )
    
    def run_stress_test(self):
        """Run stress test only"""
        print("Running stress test...")
        return self.run_test_scenario(
            "stress_test",
            users=200,
            spawn_rate=10,
            run_time="10m"
        )


def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Performance Test Runner")
    parser.add_argument("--url", default="http://localhost:8080", help="Base URL for testing")
    parser.add_argument("--scenario", choices=["all", "quick", "stress"], default="all", 
                       help="Test scenario to run")
    
    args = parser.parse_args()
    
    runner = PerformanceTestRunner(args.url)
    
    if args.scenario == "all":
        runner.run_all_scenarios()
    elif args.scenario == "quick":
        runner.run_quick_test()
    elif args.scenario == "stress":
        runner.run_stress_test()


if __name__ == "__main__":
    main()
