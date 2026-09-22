CREATE UNIQUE INDEX uk_subway_timetable_schedule
    ON subway_timetable (subway_station, line, direction, day_type, train_no, time);
