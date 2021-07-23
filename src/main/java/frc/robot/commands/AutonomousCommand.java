// RobotBuilder Version: 3.1
//
// This file was generated by RobotBuilder. It contains sections of
// code that are automatically generated and assigned by robotbuilder.
// These sections will be updated in the future when you export to
// Java from RobotBuilder. Do not put any code or make any change in
// the blocks indicating autogenerated code or it will be lost on an
// update. Deleting the comments indicating the section will prevent
// it from being updated in the future.

package frc.robot.commands;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.classes.TargetPosition2D;
import frc.robot.classes.SmoothControl;
import frc.robot.subsystems.BallPickup;
import frc.robot.subsystems.Drivetrain;
import frc.robot.subsystems.Feeder;
import frc.robot.subsystems.Launcher;

import static frc.robot.Constants.DrivetrainConstants.*;

import java.util.*;

import static frc.robot.Constants.AutonomousCommandConstants.*;


import frc.robot.classes.Kinematics;
import frc.robot.classes.Position2D;
import frc.robot.classes.SPIKE293Utils;

// BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=IMPORTS
// END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=IMPORTS

/**
 *
 */
public class AutonomousCommand extends CommandBase 
{
    // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=VARIABLE_DECLARATIONS
    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=VARIABLE_DECLARATIONS
    public enum AutoStartPosition
    {
        LEFT,
        MIDDLE,
        RIGHT
    }
    private enum AutoStates
    {
        START_SHOOT,
        WAIT_FIRST_SHOOT,
        START_PICKUP,
        PICKUP_BALLS,
        STOP_PICKUP,
        DRIVE_TO_TARGET,
        END_SHOOT,
        WAIT_END_SHOOT,
    }

    private boolean m_isDone = false;
    private Kinematics m_kinematics;
    private SmoothControl m_smoothControl;
    private Drivetrain m_drivetrain;
    private Feeder m_feeder;
    private BallPickup m_ballPickup;
    private Launcher m_launcher;

    private List<TargetPosition2D> m_ballPath = new ArrayList<TargetPosition2D>();
    private List<TargetPosition2D> m_targetPath = new ArrayList<TargetPosition2D>();
    private TargetPosition2D m_targetPose;
    private int m_targetPathIndex;
    private AutoStartPosition m_startPosition;
    private AutoStates m_state;
    private Timer m_timer;

    // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=CONSTRUCTORS
    public AutonomousCommand(Drivetrain drivetrain, Kinematics kinematics, AutoStartPosition startPosition, Feeder feeder, Launcher launcher, BallPickup ballPickup) 
    {
    // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=CONSTRUCTORS
        // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=VARIABLE_SETTING
        // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=VARIABLE_SETTING
        // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=REQUIRES
        // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=REQUIRES

        m_ballPickup = ballPickup;
        m_feeder = feeder;
        m_launcher = launcher;
        m_drivetrain = drivetrain;
        m_kinematics = kinematics;
        m_startPosition = startPosition;
        m_targetPathIndex = 0;
        addRequirements(m_drivetrain);

        //Initialize the timer
        m_timer = new Timer();
        m_timer.reset();
        m_timer.stop();
    }

    // Called when the command is initially scheduled.
    @Override
    public void initialize() 
    {
        //Setting paths 
        switch (m_startPosition) 
        {
            case LEFT:
                
                break;
            case MIDDLE:

                break;
            case RIGHT:
                m_ballPath.add(new TargetPosition2D(0, 0, Math.toRadians(0),1.0d));
                m_ballPath.add(new TargetPosition2D(10, 0, Math.toRadians(0),-1.0d));

                m_targetPath.add(new TargetPosition2D(0, 0, Math.toRadians(180), -1.0d));
                break;
            default:
                break;
        }
        
        //Initialize smooth control, reset kinematics, and set starting pose
        TargetPosition2D startingPose;
        SmartDashboard.putBoolean("AutoDone", false);

        //Initialize smooth control
        m_smoothControl = new SmoothControl();
        m_smoothControl.reset();

        //Grab the first pose and set that as our starting pose
        try 
        {
            m_targetPathIndex = 0;
            startingPose = (m_ballPath.get(m_targetPathIndex));
            System.out.println("Starting pose is:" + startingPose.getX() + ", "+ startingPose.getY() + ", "+ startingPose.getHeadingDegrees());
            m_drivetrain.initAutonomous(startingPose);
            m_targetPathIndex++;
            m_targetPose = m_ballPath.get(m_targetPathIndex);
        } 
        catch (Exception e) 
        {
            System.out.println("AutonomousCommand ERROR: target path does not contain 2 or more points!");
            m_isDone = true;
        }
        
        //Set first state
        m_state = AutoStates.START_SHOOT;
    }
    
    // Called every time the scheduler runs while the command is scheduled.
    @Override
    public void execute() 
    {   
        double vR = 0.0;
        double vL = 0.0;

        switch (m_state) 
        {
            case START_SHOOT:
                //Spin launcher up
                m_launcher.setRpm(AUTO_LAUNCHER_RPM);
                //Fire balls and wait 4 sec
                while(false == m_launcher.isReady())
                {
                    //Waiting for the shooter to get up to speed    
                }
                
                //Launcher is at speed FIRE!
                m_feeder.fire(true);

                m_state = AutoStates.WAIT_FIRST_SHOOT;
                break;
            case WAIT_FIRST_SHOOT:
                //Have we waited for 3 seconds?
                if(hasTimeElapsed(m_timer, 3.0))
                {
                    //Turn off launcher and feed motors
                    m_launcher.stop();
                    m_feeder.fire(false);
                    
                    //Move to next state
                    m_state = AutoStates.START_PICKUP;
                }
                break;
            case START_PICKUP:
                //Put down and spin pick up
                m_ballPickup.geckoToggle(true);
                m_feeder.ingest(true);
                
                m_state = AutoStates.PICKUP_BALLS;
                break;
            case PICKUP_BALLS:
                //Start auto nav drive routine
                //Compute turn rate and update range
                m_smoothControl.computeTurnRate(m_kinematics.getPose(), m_targetPose, m_drivetrain.getRobotVelocity());
                    
                try 
                {
                    //Calculate vR in feet per second
                    vR = m_ballPath.get(m_targetPathIndex-1).getVelocity() - (TRACK_WIDTH_FEET/2)*m_smoothControl.getTurnRateRadians();
                    //Calculate vL in feet per second
                    vL = m_ballPath.get(m_targetPathIndex-1).getVelocity() + (TRACK_WIDTH_FEET/2)*m_smoothControl.getTurnRateRadians();
                } 
                catch (Exception e) 
                {
                    System.out.println("AutonomousCommand ERROR: Failed to retrieve pose velocity " + (m_targetPathIndex - 1));
                    m_isDone = true;
                }
                
                SmartDashboard.putNumber("Desired Left Velocity (ft/s)", vL);
                SmartDashboard.putNumber("Desired Right Velocity (ft/s)", vR);
                SmartDashboard.putNumber("Auto Range", m_smoothControl.getRange());
                SmartDashboard.putNumber("Auto Omega Desired (Degrees)", m_smoothControl.getTurnRateDegrees());
                SmartDashboard.putString("Next Target", m_targetPose.getX() + ", "+ m_targetPose.getY() + ", "+ m_targetPose.getHeadingDegrees());

                //Converting ft/s equation output to controller velocity
                vR = SPIKE293Utils.feetPerSecToControllerVelocity(vR);
                vL = SPIKE293Utils.feetPerSecToControllerVelocity(vL);

                //Send vR and vL to velocity drive, units are in controller velocity
                m_drivetrain.velocityDrive(vL, vR);

                //Have we reached the target?
                if(TARGET_WITHIN_RANGE_FEET >= m_smoothControl.getRange())
                {
                    //Get next target pose
                    m_targetPathIndex++;

                    if(m_targetPathIndex >= m_ballPath.size())
                    {
                        //No more poses to move to
                        System.out.println("Autonav done.");
                        SmartDashboard.putBoolean("AutoDone", true);
                        m_drivetrain.stop();        //Stop all motors
                        m_state = AutoStates.STOP_PICKUP;   //when finished, stop pickup  
                    }
                    else
                    {
                        m_targetPose = m_ballPath.get(m_targetPathIndex);
                        System.out.println("Moving to next target." + m_targetPose.getX() + ", "+ m_targetPose.getY() + ", "+ m_targetPose.getHeadingDegrees());
                    }
                }

                break;
            case STOP_PICKUP:
                m_ballPickup.geckoToggle(false);
                m_feeder.ingest(false);
                
                //Setting up for next path
                m_targetPath.add(0, new TargetPosition2D(m_kinematics.getPose().getX(), m_kinematics.getPose().getY(), m_kinematics.getPose().getHeadingRadians(), -1.0d));
                m_targetPathIndex = 1;
                m_targetPose = m_targetPath.get(m_targetPathIndex);
                m_state = AutoStates.DRIVE_TO_TARGET;
                
                break;
            case DRIVE_TO_TARGET:
                //Start auto nav drive routine
                //Compute turn rate and update range
                m_smoothControl.computeTurnRate(m_kinematics.getPose(), m_targetPose, m_drivetrain.getRobotVelocity());
                    
                try 
                {
                    //Calculate vR in feet per second
                    vR = m_targetPath.get(m_targetPathIndex-1).getVelocity() - (TRACK_WIDTH_FEET/2)*m_smoothControl.getTurnRateRadians();
                    //Calculate vL in feet per second
                    vL = m_targetPath.get(m_targetPathIndex-1).getVelocity() + (TRACK_WIDTH_FEET/2)*m_smoothControl.getTurnRateRadians();
                } 
                catch (Exception e) 
                {
                    System.out.println("AutonomousCommand ERROR: Failed to retrieve pose velocity " + (m_targetPathIndex - 1));
                    m_isDone = true;
                }
                
                SmartDashboard.putNumber("Desired Left Velocity (ft/s)", vL);
                SmartDashboard.putNumber("Desired Right Velocity (ft/s)", vR);
                SmartDashboard.putNumber("Auto Range", m_smoothControl.getRange());
                SmartDashboard.putNumber("Auto Omega Desired (Degrees)", m_smoothControl.getTurnRateDegrees());
                SmartDashboard.putString("Next Target", m_targetPose.getX() + ", "+ m_targetPose.getY() + ", "+ m_targetPose.getHeadingDegrees());

                //Converting ft/s equation output to controller velocity
                vR = SPIKE293Utils.feetPerSecToControllerVelocity(vR);
                vL = SPIKE293Utils.feetPerSecToControllerVelocity(vL);

                //Send vR and vL to velocity drive, units are in controller velocity
                m_drivetrain.velocityDrive(vL, vR);

                //Have we reached the target?
                if(TARGET_WITHIN_RANGE_FEET >= m_smoothControl.getRange())
                {
                    //Get next target pose
                    m_targetPathIndex++;

                    if(m_targetPathIndex >= m_targetPath.size())
                    {
                        //No more poses to move to
                        System.out.println("Autonav done.");
                        SmartDashboard.putBoolean("AutoDone", true);
                        m_drivetrain.stop();        //Stop all motors
                        m_state = AutoStates.END_SHOOT;   //when finished, stop pickup  
                    }
                    else
                    {
                        m_targetPose = m_targetPath.get(m_targetPathIndex);
                        System.out.println("Moving to next target." + m_targetPose.getX() + ", "+ m_targetPose.getY() + ", "+ m_targetPose.getHeadingDegrees());
                    }
                }

                break;

                case END_SHOOT:
                    //Spin launcher up
                    m_launcher.setRpm(AUTO_LAUNCHER_RPM);
                    //Fire balls and wait 4 sec
                    while(false == m_launcher.isReady())
                    {
                        //Waiting for the shooter to get up to speed    
                    }
                    
                    //Launcher is at speed FIRE!
                    m_feeder.fire(true);

                    //Set state to waiting
                    m_state = AutoStates.WAIT_END_SHOOT;
                    break;
            case WAIT_END_SHOOT:
                //Have we waited for 3 seconds?
                if(hasTimeElapsed(m_timer, 3.0))
                {
                    //Turn off launcher and feed motors
                    m_launcher.stop();
                    m_feeder.fire(false);

                    m_isDone = true;
                }
                break;
            default:
                //No valid state was set
                break;
        } 
    }

    // Handles whether or not enough time has elapsed
    // Requires that the timer has been reset the first time this is called
    // Will not work properly if the timer is manipulated outside of this function!
    private boolean hasTimeElapsed(Timer timer, double secondsToWait)
    {
        boolean isDone = false;

        timer.start(); //No-op if this timer is already running
        if(secondsToWait <= m_timer.get())
        {
            timer.stop();
            timer.reset();
            isDone = true;
        }

        return isDone;
    }

    // Called once the command ends or is interrupted.
    @Override
    public void end(boolean interrupted) 
    {
        m_targetPathIndex = 0;
        m_isDone = false;
        m_drivetrain.stop();
    }

    // Returns true when the command should end.
    @Override
    public boolean isFinished() 
    {
        return m_isDone;
    }

    @Override
    public boolean runsWhenDisabled() 
    {
        // BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DISABLED
        return false;

        // END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID=DISABLED
    }

    public Position2D getStartingPose()
    {
        return m_ballPath.get(0);
    }
}
