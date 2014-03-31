//
//  ESTStreamRSSIViewController.m
//  Examples
//
//  Created by Grzegorz on 3/31/14.
//  Copyright (c) 2014 com.estimote. All rights reserved.
//

#import "ESTStreamRSSIVC.h"
#import "ESTBeaconManager.h"

@interface ESTStreamRSSIVC () <ESTBeaconManagerDelegate>

@property (nonatomic, strong) ESTBeaconManager  *beaconManager;
@property (nonatomic, strong) ESTBeaconRegion   *beaconRegion;
@property (nonatomic, strong) NSArray *beaconsArray;

//@property (nonatomic, strong) UIImageView       *imageView;
@property (nonatomic, strong) UILabel           *zoneLabel;

@end

@implementation ESTStreamRSSIVC

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    /*
     * UI setup.
     */
    self.view.backgroundColor = [UIColor whiteColor];
    
    self.zoneLabel = [[UILabel alloc] initWithFrame:CGRectMake(0,
                                                               50,
                                                               self.view.frame.size.width,
                                                               400)];
    self.zoneLabel.textAlignment = NSTextAlignmentCenter;
    self.zoneLabel.numberOfLines = 10;
    [self.view addSubview:self.zoneLabel];
    
    /*
     * BeaconManager setup.
     */
    self.beaconManager = [[ESTBeaconManager alloc] init];
    self.beaconManager.delegate = self;
    
    self.beaconRegion = [[ESTBeaconRegion alloc] initWithProximityUUID:ESTIMOTE_PROXIMITY_UUID
                                                            identifier:@"EstimoteSampleRegion"];
    [self.beaconManager startRangingBeaconsInRegion:self.beaconRegion];
}

- (void)viewDidDisappear:(BOOL)animated
{
    [self.beaconManager stopRangingBeaconsInRegion:self.beaconRegion];
    
    [super viewDidDisappear:animated];
}


- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - ESTBeaconManager delegate

- (void)beaconManager:(ESTBeaconManager *)manager didRangeBeacons:(NSArray *)beacons inRegion:(ESTBeaconRegion *)region
{
    self.beaconsArray = beacons;
    [self sortBeaconsArray];
    if (self.beaconsArray.count > 0)
    {
        __block NSString* text = [NSString stringWithFormat:@"%i iBeacons", beacons.count];
        [self.beaconsArray enumerateObjectsUsingBlock:^(ESTBeacon* beacon, NSUInteger idx, BOOL *stop) {
            text = [text stringByAppendingFormat:@"\nMajor: %@, Minor: %@, RSSI: %i", beacon.major, beacon.minor, beacon.rssi];
        }];
        
        self.zoneLabel.text     =  text;
        [self broadcastBeaconsInfo];
    }
}

- (void)sortBeaconsArray
{
    id sortingFunction = ^NSComparisonResult(ESTBeacon* b1, ESTBeacon* b2) {
        NSComparisonResult r1 = [b1.major compare:b2.major];
        if (r1 == NSOrderedSame) {
            return [b1.minor compare:b2.minor];
        } else {
            return r1;
        }
    };
    self.beaconsArray = [self.beaconsArray sortedArrayUsingComparator: sortingFunction];
}

- (void)broadcastBeaconsInfo
{
    [self.beaconsArray enumerateObjectsUsingBlock:^(ESTBeacon* beacon, NSUInteger idx, BOOL *stop) {
        [self broadcastBeaconInfo:beacon];
    }];
}

- (void)broadcastBeaconInfo: (ESTBeacon*) beacon
{
    NSString *post = [NSString stringWithFormat:@"beaconMajor=%@&beaconMinor=%@&beaconRSSI=%i", beacon.major, beacon.minor, beacon.rssi];
    NSData *postData = [post dataUsingEncoding:NSASCIIStringEncoding allowLossyConversion:YES];
    NSString *postLength = [NSString stringWithFormat:@"%d", [postData length]];
    
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setURL:[NSURL URLWithString:@"http://192.168.0.13:9000/beaconInfo"]];
    [request setHTTPMethod:@"POST"];
    [request setValue:postLength forHTTPHeaderField:@"Content-Length"];
    [request setValue:@"application/x-www-form-urlencoded;charset=UTF-8" forHTTPHeaderField:@"Content-Type"];
    [request setHTTPBody:postData];
    
    NSURLResponse *response;
    NSData *POSTReply = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:nil];
    NSString *theReply = [[NSString alloc] initWithBytes:[POSTReply bytes] length:[POSTReply length] encoding: NSASCIIStringEncoding];
    NSLog(@"Reply: %@", theReply);
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
