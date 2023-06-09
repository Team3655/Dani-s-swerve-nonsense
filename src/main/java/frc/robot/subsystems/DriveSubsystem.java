// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

// DISCLAIMER! THIS DRIVE_SUBSYSTEM HAS MANY BUGS AND SHOULD NOT BE USED AS A REFRANCE!

package frc.robot.subsystems;

import com.ctre.phoenix.sensors.WPI_Pigeon2;

import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.BaseModuleConstants;
import frc.robot.Constants.DriveConstants;
import frc.robot.Mechanisms.SwerveModule;
import frc.robot.TractorToolbox.JoystickUtils;
import frc.robot.TractorToolbox.LimelightHelpers;

public class DriveSubsystem extends SubsystemBase {

	private boolean fieldRelative = true;

	private final SwerveModule frontLeft;
	private final SwerveModule frontRight;
	private final SwerveModule rearLeft;
	private final SwerveModule rearRight;

	private SwerveModulePosition[] swervePositions;

	// Initalizing the gyro sensor
	private final WPI_Pigeon2 gyro;

	// Odeometry class for tracking robot pose
	SwerveDriveOdometry odometry;

	private Field2d field;

	private SwerveDrivePoseEstimator poseEstimator;

	/** Creates a new DriveSubsystem. */
	public DriveSubsystem() {

		frontLeft = new SwerveModule(
				"FL",
				BaseModuleConstants.kFrontLeftTurningMotorPort,
				BaseModuleConstants.kPrimaryFrontLeftDriveMotorPort,
				BaseModuleConstants.kSecondaryFrontLeftDriveMotorPort,
				BaseModuleConstants.kFrontLeftAbsoluteEncoderPort,
				BaseModuleConstants.kFrontLeftAngleZero,
				BaseModuleConstants.kModuleTurningGains,
				BaseModuleConstants.kModuleDriveGains);

		frontRight = new SwerveModule(
				"FR",
				BaseModuleConstants.kFrontRightTurningMotorPort,
				BaseModuleConstants.kPrimaryFrontRightDriveMotorPort,
				BaseModuleConstants.kSecondaryFrontRightDriveMotorPort,
				BaseModuleConstants.kFrontRightAbsoluteEncoderPort,
				BaseModuleConstants.kFrontRightAngleZero,
				BaseModuleConstants.kModuleTurningGains,
				BaseModuleConstants.kModuleDriveGains);

		rearLeft = new SwerveModule(
				"RL",
				BaseModuleConstants.kRearLeftTurningMotorPort,
				BaseModuleConstants.kPrimaryRearLeftDriveMotorPort,
				BaseModuleConstants.kSecondaryRearLeftDriveMotorPort,
				BaseModuleConstants.kRearLeftAbsoluteEncoderPort,
				BaseModuleConstants.kRearLeftAngleZero,
				BaseModuleConstants.kModuleTurningGains,
				BaseModuleConstants.kModuleDriveGains);

		rearRight = new SwerveModule(
				"RR",
				BaseModuleConstants.kRearRightTurningMotorPort,
				BaseModuleConstants.kPrimaryRearRightDriveMotorPort,
				BaseModuleConstants.kSecondaryRearRightDriveMotorPort,
				BaseModuleConstants.kRearRightAbsoluteEncoderPort,
				BaseModuleConstants.kRearRightAngleZero,
				BaseModuleConstants.kModuleTurningGains,
				BaseModuleConstants.kModuleDriveGains);

		swervePositions = new SwerveModulePosition[] {
				frontLeft.getPosition(),
				frontRight.getPosition(),
				rearLeft.getPosition(),
				rearRight.getPosition()
		};

		gyro = new WPI_Pigeon2(DriveConstants.kPigeonPort, Constants.kDriveCANBusName);
		gyro.setYaw(0);

		odometry = new SwerveDriveOdometry(
				DriveConstants.kDriveKinematics,
				gyro.getRotation2d(),
				swervePositions);

		field = new Field2d();

		poseEstimator = new SwerveDrivePoseEstimator(
				DriveConstants.kDriveKinematics,
				gyro.getRotation2d(),
				swervePositions,
				new Pose2d());
	}

	@Override
	public void periodic() {
		// This method will be called once per scheduler run
		
		updateOdometry();

		SmartDashboard.putNumber("FL Absolute", frontLeft.getAbsoluteHeading());
		SmartDashboard.putNumber("FR Absolute", frontRight.getAbsoluteHeading());
		SmartDashboard.putNumber("RL Absolute", rearLeft.getAbsoluteHeading());
		SmartDashboard.putNumber("RR Absolute", rearRight.getAbsoluteHeading());

		SmartDashboard.putNumber("Gyro yaw", gyro.getYaw());
		SmartDashboard.putNumber("Gyro pitch", gyro.getPitch());
		SmartDashboard.putNumber("Gyro roll", gyro.getRoll());

		SmartDashboard.putNumber("FL Meters", frontLeft.getDistanceMeters());
		SmartDashboard.putNumber("FR Meters", frontRight.getDistanceMeters());
		SmartDashboard.putNumber("RL Meters", rearLeft.getDistanceMeters());
		SmartDashboard.putNumber("RR Meters", rearRight.getDistanceMeters());

		SmartDashboard.putData("field", field);
		SmartDashboard.putNumber("2D Gyro", odometry.getPoseMeters().getRotation().getDegrees());
		SmartDashboard.putNumber("2D X", getPose().getX());
		SmartDashboard.putNumber("2D Y", getPose().getY());
	}

	// region getters
	public double getHeading() {
		return gyro.getRotation2d().getDegrees();
	}

	public double getHeading360() {
		return (gyro.getRotation2d().getDegrees() % 360);
	}

	public double getRoll() {
		return gyro.getRoll();
	}

	public double getPitch() {
		return gyro.getPitch();
	}

	public double getTurnRate() {
		return gyro.getRate() * (DriveConstants.kGyroReversed ? -1.0 : 1.0);
	}

	public Pose2d getPose() {
		return odometry.getPoseMeters();
	}

	public Pose2d getPoseEstimatorPose2d() {
		return poseEstimator.getEstimatedPosition();
	}

	public void resetOdometry(Pose2d pose) {
		odometry.resetPosition(
				gyro.getRotation2d(),
				swervePositions,
				pose);
	}
	// endregion

	// region setter

	public void lockWheels() {
		double rot = DriveConstants.kMaxRPM;

		SwerveModuleState[] swerveModuleStates = DriveConstants.kDriveKinematics.toSwerveModuleStates(
				new ChassisSpeeds(0, 0, rot));

		SwerveDriveKinematics.desaturateWheelSpeeds(
				swerveModuleStates, 0);

		setModuleStates(swerveModuleStates, false);
	}

	public void robotCentricDrive(double xSpeed, double ySpeed, double rot) {
		setFieldCentric(false);
		codeDrive(xSpeed, ySpeed, rot);
		setFieldCentric(true);
	}

	public void codeDrive(double xSpeed, double ySpeed, double rot) {
		Translation2d dir = new Translation2d(xSpeed, ySpeed);
		drive(dir, rot, false, false);
	}

	public void driverDrive(double xSpeed, double ySpeed, double rot, boolean isTurbo, boolean isSneak) {
		Translation2d translation = new Translation2d(xSpeed, ySpeed);
		translation = JoystickUtils.curveTranslation2d(translation);
		drive(translation, rot, isTurbo, isSneak);
	}

	public void drive(Translation2d translation, double rotation, boolean isTurbo, boolean isSneak) {

		double maxSpeed;

		if (isSneak) {
			maxSpeed = DriveConstants.kMaxSneakMetersPerSecond;
		} else {
			maxSpeed = DriveConstants.kMaxSpeedMetersPerSecond;	
		}

		double xSpeed = maxSpeed * translation.getX();
		double ySpeed = maxSpeed * translation.getY();

		rotation *= DriveConstants.kMaxRPM;

		ChassisSpeeds chassisSpeeds = new ChassisSpeeds(xSpeed, ySpeed, rotation);

		SwerveModuleState[] swerveModuleStates = DriveConstants.kDriveKinematics.toSwerveModuleStates(
				fieldRelative
						? ChassisSpeeds.fromFieldRelativeSpeeds(chassisSpeeds, gyro.getRotation2d())
						: new ChassisSpeeds(xSpeed, ySpeed, rotation));

		setModuleStates(swerveModuleStates, isTurbo);
	}

	public void setModuleStates(SwerveModuleState[] desiredStates) {
		setModuleStates(desiredStates, false);
	}

	public void setModuleStates(SwerveModuleState[] desiredStates, boolean isTurbo) {
		SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, BaseModuleConstants.kMaxModuleSpeedMetersPerSecond);

		frontLeft.setDesiredState(desiredStates[0], isTurbo);
		frontRight.setDesiredState(desiredStates[1], isTurbo);
		rearLeft.setDesiredState(desiredStates[2], isTurbo);
		rearRight.setDesiredState(desiredStates[3], isTurbo);
	}

	public void updateOdometry() {
		swervePositions = new SwerveModulePosition[] {
				frontLeft.getPosition(),
				frontRight.getPosition(),
				rearLeft.getPosition(),
				rearRight.getPosition()
		};

		odometry.update(
				Rotation2d.fromDegrees(getHeading()),
				swervePositions);

		poseEstimator.update(gyro.getRotation2d(), swervePositions);

		if (LimelightHelpers.getTV("")) {
			Pose2d llPose2d = LimelightHelpers.getBotPose2d_wpiRed("");
			double latency = Units.millisecondsToSeconds(LimelightHelpers.getLatency_Capture("") - LimelightHelpers.getLatency_Pipeline(""));
			double timeStamp = Timer.getFPGATimestamp() - latency;
			poseEstimator.addVisionMeasurement(
				llPose2d, 
				timeStamp); 
		}

		field.setRobotPose(odometry.getPoseMeters());
	}

	public void resetEncoders() {
		frontLeft.resetAbsoluteEncoder();
		rearLeft.resetAbsoluteEncoder();
		frontRight.resetAbsoluteEncoder();
		rearRight.resetAbsoluteEncoder();
	}

	public void zeroHeading() {
		gyro.reset();
	}

	public void setHeading(double heading) {
		gyro.setYaw(heading);
	}

	public CommandBase toggleFieldCentric() {
		return runOnce(() -> {
			fieldRelative = !fieldRelative;
		});
	}

	public void setFieldCentric(boolean fieldCentric) {
		fieldRelative = fieldCentric;
	}

	public void stopMotors() {
		frontLeft.stopMotors();
		frontRight.stopMotors();
		rearLeft.stopMotors();
		rearRight.stopMotors();
	}

	// endregion

}
